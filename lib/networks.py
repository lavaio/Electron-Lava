# Electron Cash - lightweight Bitcoin Cash client
# Copyright (C) 2011 thomasv@gitorious
# Copyright (C) 2017 Neil Booth
#
# Permission is hereby granted, free of charge, to any person
# obtaining a copy of this software and associated documentation files
# (the "Software"), to deal in the Software without restriction,
# including without limitation the rights to use, copy, modify, merge,
# publish, distribute, sublicense, and/or sell copies of the Software,
# and to permit persons to whom the Software is furnished to do so,
# subject to the following conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
# BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
# ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

import json, pkgutil

def _read_json_dict(filename):
    try:
        data = pkgutil.get_data(__name__, filename)
        r = json.loads(data.decode('utf-8'))
    except:
        r = {}
    return r

class AbstractNet:
    TESTNET = False


class MainNet(AbstractNet):
    TESTNET = False
    WIF_PREFIX = 0x80
    ADDRTYPE_P2PKH = 0
    ADDRTYPE_P2PKH_BITPAY = 28
    ADDRTYPE_P2SH = 5
    ADDRTYPE_P2SH_BITPAY = 40
    CASHADDR_PREFIX = "bitcoincash"
    SEGWIT_HRP = "bc"
    #HEADERS_URL = "http://bitcoincash.com/files/blockchain_headers"
    GENESIS = "dfc8e3d348da67cf64fef22c927e593860465ada0546fa1719556958b95c7cf6"
    DEFAULT_PORTS = {'t': '60998', 's': '60999'}
    DEFAULT_SERVERS = _read_json_dict('servers.json')  # DO NOT MODIFY IN CLIENT CODE
    #TITLE = 'Electron Cash'
    TITLE = 'Electron Lava'
    # block header add 20 bytes
    HDR_V4_SIZE = 156
    HDR_V4_HEIGHT = 67584
    HDR_V4_OLD_LENGTH = HDR_V4_HEIGHT * 136

    # Bitcoin Cash fork block specification
    #BITCOIN_CASH_FORK_BLOCK_HEIGHT = 478559
    #BITCOIN_CASH_FORK_BLOCK_HASH = "000000000000000000651ef99cb9fcbe0dadde1d424bd9f15ff20136191a5eec"

    # Note: this is not the Merkle root of the verification block itself , but a Merkle root of
    # all blockchain headers up until and including this block. To get this value you need to
    # connect to an ElectrumX server you trust and issue it a protocol command. This can be
    # done in the console as follows:
    #
    #    network.synchronous_get(("blockchain.block.header", [height, height]))
    #
    # Consult the ElectrumX documentation for more details.
    VERIFICATION_BLOCK_MERKLE_ROOT = "0ba34c8de9be42563ebe6fb10bc687e4f19f19f6cb565cc75536afdc91e43d0b"
    VERIFICATION_BLOCK_HEIGHT = 0

    # Version numbers for BIP32 extended keys
    # standard: xprv, xpub
    XPRV_HEADERS = {
        'standard':    0x0488ade4,  # xprv
        'p2wpkh-p2sh': 0x049d7878,  # yprv
        'p2wsh-p2sh':  0x0295b005,  # Yprv
        'p2wpkh':      0x04b2430c,  # zprv
        'p2wsh':       0x02aa7a99,  # Zprv
    }

    XPUB_HEADERS = {
        'standard':    0x0488b21e,  # xpub
        'p2wpkh-p2sh': 0x049d7cb2,  # ypub
        'p2wsh-p2sh':  0x0295b43f,  # Ypub
        'p2wpkh':      0x04b24746,  # zpub
        'p2wsh':       0x02aa7ed3,  # Zpub
    }


class TestNet(AbstractNet):
    TESTNET = True
    WIF_PREFIX = 0xef
    ADDRTYPE_P2PKH = 111
    ADDRTYPE_P2PKH_BITPAY = 111  # Unsure
    ADDRTYPE_P2SH = 196
    ADDRTYPE_P2SH_BITPAY = 196  # Unsure
    CASHADDR_PREFIX = "bchtest"
    SEGWIT_HRP = "tb"
    #HEADERS_URL = "http://bitcoincash.com/files/testnet_headers"
    GENESIS = "54746281650914f19f4b4e80002c4a9b7ab6e2334b25d6e141bc9130ea4ec572"
    DEFAULT_PORTS = {'t':'20998', 's':'20999'}
    DEFAULT_SERVERS = _read_json_dict('servers_testnet.json')  # DO NOT MODIFY IN CLIENT CODE
    TITLE = 'Electron Lava Testnet'
    # block header add 20 bytes
    HDR_V4_SIZE = 156
    HDR_V4_HEIGHT = 13115
    HDR_V4_OLD_LENGTH = HDR_V4_HEIGHT * 136

    # Bitcoin Cash fork block specification
    #BITCOIN_CASH_FORK_BLOCK_HEIGHT = 1155876
    #BITCOIN_CASH_FORK_BLOCK_HASH = "00000000000e38fef93ed9582a7df43815d5c2ba9fd37ef70c9a0ea4a285b8f5"

    VERIFICATION_BLOCK_MERKLE_ROOT = "3adb98d48ff23bf6f75972faca104652538b38b8c343159aa88a223fa9e79029"
    VERIFICATION_BLOCK_HEIGHT = 0

    # Version numbers for BIP32 extended keys
    # standard: tprv, tpub
    XPRV_HEADERS = {
        'standard':    0x04358394,  # tprv
        'p2wpkh-p2sh': 0x044a4e28,  # uprv
        'p2wsh-p2sh':  0x024285b5,  # Uprv
        'p2wpkh':      0x045f18bc,  # vprv
        'p2wsh':       0x02575048,  # Vprv
    }

    XPUB_HEADERS = {
        'standard':    0x043587cf,  # tpub
        'p2wpkh-p2sh': 0x044a5262,  # upub
        'p2wsh-p2sh':  0x024289ef,  # Upub
        'p2wpkh':      0x045f1cf6,  # vpub
        'p2wsh':       0x02575483,  # Vpub
    }


# All new code should access this to get the current network config.
net = MainNet
#net = TestNet

def set_mainnet():
    global net
    net = MainNet

def set_testnet():
    global net
    net = TestNet


# Compatibility
def _instancer(cls):
    return cls()

@_instancer
class NetworkConstants:
    ''' Compatibility class for old code such as extant plugins.

    Client code can just do things like:
    NetworkConstants.ADDRTYPE_P2PKH, NetworkConstants.DEFAULT_PORTS, etc.

    We have transitioned away from this class. All new code should use the
    'net' global variable above instead. '''
    def __getattribute__(self, name):
        return getattr(net, name)

    def __setattr__(self, name, value):
        raise RuntimeError('NetworkConstants does not support setting attributes! ({}={})'.format(name,value))
        #setattr(net, name, value)
