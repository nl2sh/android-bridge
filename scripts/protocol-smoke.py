#!/usr/bin/env python3
"""Read-only Binder protocol regression; run against an installed debug APK."""
import argparse
import base64
import json
import re
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb', default='adb')
parser.add_argument('--serial', required=True)
args = parser.parse_args()
adb = [args.adb, '-s', args.serial]


def call(method, payload=None):
    command = adb + ['shell', 'content', 'call', '--uri',
                     'content://com.nl2sh.bridge.ops', '--method', method]
    if payload is not None:
        command += ['--extra', 'payload_b64:s:' + payload]
    output = subprocess.check_output(command, text=True, stderr=subprocess.STDOUT)
    match = re.search(r'data=([A-Za-z0-9_-]+)', output)
    assert match, output
    return json.loads(base64.urlsafe_b64decode(match[1] + '=' * (-len(match[1]) % 4)))


def encode(value):
    return base64.urlsafe_b64encode(json.dumps(value, ensure_ascii=False).encode()).decode().rstrip('=')


legacy = call('capabilities')
assert legacy['ok'] and legacy['protocol_version'] == 2
assert legacy['limits']['tree_nodes'] == 250
assert legacy['limits']['reply_bytes'] == 65536
assert ('ime.commit.v1' in legacy['capabilities']) == legacy['services']['ime']
assert ('ui.tree.v2' in legacy['capabilities']) == legacy['services']['accessibility']
request = dict(protocol=2, request_id='中文:%3A%25', method='capabilities', args={})
reply = call('invoke', encode(request))
assert reply['ok'] and reply['request_id'] == request['request_id']
assert reply['protocol'] == 2 and reply['result']['services'] == legacy['services']
for change in [dict(protocol=1), dict(protocol=2.5), dict(protocol='2'),
               dict(request_id=''), dict(request_id=42), dict(request_id='x' * 129),
               dict(args=[]), dict(method='invoke'), dict(method=42)]:
    assert not call('invoke', encode(request | change))['ok'], change
for malformed in ['!', 'e30', 'x' * 32769, '_w']:
    assert not call('invoke', malformed)['ok'], malformed[:20]
unknown = call('invoke', encode(request | dict(method='no_such_action')))
assert not unknown['ok'] and unknown['request_id'] == request['request_id']
# Android permission denial must occur before provider dispatch for an ordinary app UID.
output = subprocess.run(adb + ['shell', 'run-as', 'com.nl2sh.bridge',
    'content', 'call', '--uri', 'content://com.nl2sh.bridge.ops', '--method', 'capabilities'],
    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
assert 'data=' not in output.stdout and ('Permission' in output.stdout
    or 'SecurityException' in output.stdout), output.stdout
print('PASS: discovery, independent service flags, v2 correlation, invalid requests, app UID denial')
