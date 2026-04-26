"""
NTorq XT SmartXonnect BLE Protocol Handler
Implements: Frame builder, XOR encryption, checksum calculation
"""


class NTorqProtocol:
    # Encryption key pattern
    XOR_KEY = 0xEA

    # Frame markers
    FRAME_START = 0x5A
    FRAME_END = 0xFF

    @staticmethod
    def xor_encrypt(data: bytes) -> bytes:
        """XOR encrypt payload with 0xEA pattern"""
        return bytes([b ^ NTorqProtocol.XOR_KEY for b in data])

    @staticmethod
    def xor_decrypt(data: bytes) -> bytes:
        """XOR decrypt payload (same as encrypt for XOR)"""
        return NTorqProtocol.xor_encrypt(data)

    @staticmethod
    def calculate_checksum(frame_bytes: bytes) -> int:
        """
        Calculate checksum for frame
        checksum = 255 - (sum_of_bytes % 256)
        """
        total = sum(frame_bytes)
        checksum = 255 - (total % 256)
        return checksum & 0xFF

    @staticmethod
    def make_frame(opcode: int, payload: bytes = b'') -> bytes:
        """
        Build complete BLE frame
        Format: [0x5A] [OPCODE] [ENCRYPTED_PAYLOAD] [CHECKSUM] [0xFF]
        """
        header = bytes([NTorqProtocol.FRAME_START, opcode])
        encrypted = NTorqProtocol.xor_encrypt(payload)
        frame_for_checksum = header + encrypted
        checksum = NTorqProtocol.calculate_checksum(frame_for_checksum)

        complete_frame = frame_for_checksum + bytes([checksum, NTorqProtocol.FRAME_END])
        return complete_frame

    @staticmethod
    def parse_frame(data: bytes) -> dict:
        """
        Parse incoming BLE frame and extract data
        Returns: {'opcode': int, 'payload': bytes, 'valid': bool, 'raw': bytes}
        """
        if len(data) < 4:
            return {'valid': False, 'raw': data, 'error': 'Frame too short'}

        if data[0] != NTorqProtocol.FRAME_START or data[-1] != NTorqProtocol.FRAME_END:
            return {'valid': False, 'raw': data, 'error': 'Invalid frame markers'}

        opcode = data[1]
        encrypted_payload = data[2:-2]
        received_checksum = data[-2]

        # Verify checksum
        frame_for_checksum = data[:2] + encrypted_payload
        calculated_checksum = NTorqProtocol.calculate_checksum(frame_for_checksum)

        if received_checksum != calculated_checksum:
            return {'valid': False, 'raw': data, 'error': f'Checksum mismatch: got {received_checksum}, expected {calculated_checksum}'}

        # Decrypt payload
        decrypted = NTorqProtocol.xor_decrypt(encrypted_payload)

        return {
            'valid': True,
            'opcode': opcode,
            'payload': decrypted,
            'encrypted_payload': encrypted_payload,
            'checksum': received_checksum,
            'raw': data
        }
