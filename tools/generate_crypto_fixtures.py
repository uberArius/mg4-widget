"""Generate public protocol vectors by running the pinned upstream implementation.

python -m pip install --target .tools/python pycryptodome
python tools/generate_crypto_fixtures.py
No account, token or network request is used.
"""
import datetime
import importlib.util
import json
from pathlib import Path
import sys
import types

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / '.tools/python'))
SOURCE = ROOT / 'reference/saic-python-client-ng/src/saic_ismart_client_ng'
for name, path in [('saic_ismart_client_ng', SOURCE), ('saic_ismart_client_ng.net', SOURCE / 'net')]:
    module = types.ModuleType(name)
    module.__path__ = [str(path)]
    sys.modules[name] = module
from saic_ismart_client_ng.net.crypto import encrypt_request, encrypt_response

base = 'https://gateway-mg-eu.soimt.com/api.app/v1/'
timestamp = 1700000000000
cases = []
for path, body, content_type, token in [
    ('/oauth/token', 'grant_type=password&username=test%40example.com&password=not-a-real-password', 'application/x-www-form-urlencoded', ''),
    ('/vehicle/status?vin=example&vehStatusReqType=2', '', 'application/json', 'test-token'),
    ('/vehicle/control', '{"rvcReqType":"6","vin":"example","rvcParams":[]}', 'application/json', 'test-token'),
]:
    encrypted, headers = encrypt_request(original_request_url=base + path[1:],
        original_request_headers={'Content-Type': content_type}, original_request_content=body,
        request_timestamp=datetime.datetime.fromtimestamp(timestamp / 1000, datetime.timezone.utc),
        base_uri=base, region='eu', tenant_id='459771', user_token=token)
    cases.append(dict(path=path, body=body, type=content_type, token=token, timestamp=str(timestamp),
        encrypted=encrypted.decode() if isinstance(encrypted, bytes) else encrypted,
        signature=headers['APP-VERIFICATION-STRING']))
plain = '{"code":0,"data":{"statusTime":1700000000,"basicVehicleStatus":{"extendedData1":76}}}'
encrypted, headers = encrypt_response(original_request_url=base + 'vehicle/status', original_response_headers={'Content-Type':'application/json'},
    original_response_content=plain, response_timestamp_ms=timestamp, base_uri=base, tenant_id='459771')
out = ROOT / 'app/src/test/resources/crypto-vectors.json'
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(dict(requests=cases, response=dict(plain=plain, encrypted=encrypted.decode(), timestamp=str(timestamp), type='application/json')), indent=2), encoding='utf-8')
print('Generated 3 request vectors and 1 response vector from upstream.')
