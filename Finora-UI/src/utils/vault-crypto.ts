/**
 * Client-side vault key derivation using WebCrypto API.
 * Derives a 256-bit AES key from a passphrase using PBKDF2.
 */

const PBKDF2_ITERATIONS = 310_000;
const KEY_LENGTH_BITS = 256;

/**
 * Derive a vault key from a passphrase and salt using PBKDF2.
 * @param passphrase User's vault passphrase
 * @param saltBase64 Base64-encoded salt from server
 * @returns Base64-encoded derived key to send in X-Vault-Key header
 */
export async function deriveVaultKey(passphrase: string, saltBase64: string): Promise<string> {
  const encoder = new TextEncoder();
  const salt = base64ToArrayBuffer(saltBase64);

  // Import passphrase as a CryptoKey
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    encoder.encode(passphrase),
    'PBKDF2',
    false,
    ['deriveBits']
  );

  // Derive key bits using PBKDF2
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt,
      iterations: PBKDF2_ITERATIONS,
      hash: 'SHA-256',
    },
    keyMaterial,
    KEY_LENGTH_BITS
  );

  // Convert to Base64 for transmission
  return arrayBufferToBase64(derivedBits);
}

/**
 * Convert Base64 string to ArrayBuffer.
 */
function base64ToArrayBuffer(base64: string): ArrayBuffer {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

/**
 * Convert ArrayBuffer to Base64 string.
 */
function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}
