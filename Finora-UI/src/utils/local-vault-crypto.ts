const PBKDF2_ITERATIONS = 310_000;
const KEY_LENGTH_BITS = 256;
const SALT_LENGTH = 16;
const IV_LENGTH = 12;
const GCM_TAG_BITS = 128;

function base64ToArrayBuffer(base64: string): ArrayBuffer {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
  return btoa(binary);
}

async function deriveKey(passphrase: string, salt: Uint8Array): Promise<CryptoKey> {
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(passphrase),
    'PBKDF2',
    false,
    ['encrypt', 'decrypt']
  );

  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', salt: salt as BufferSource, iterations: PBKDF2_ITERATIONS, hash: 'SHA-256' },
    keyMaterial,
    { name: 'AES-GCM', length: KEY_LENGTH_BITS },
    false,
    ['encrypt', 'decrypt']
  );
}

export async function encryptVault(data: string, passphrase: string): Promise<ArrayBuffer> {
  const salt = crypto.getRandomValues(new Uint8Array(SALT_LENGTH));
  const iv = crypto.getRandomValues(new Uint8Array(IV_LENGTH));
  const key = await deriveKey(passphrase, salt);

  const ciphertext = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv, tagLength: GCM_TAG_BITS },
    key,
    new TextEncoder().encode(data)
  );

  const result = new Uint8Array(SALT_LENGTH + IV_LENGTH + ciphertext.byteLength);
  result.set(salt, 0);
  result.set(iv, SALT_LENGTH);
  result.set(new Uint8Array(ciphertext), SALT_LENGTH + IV_LENGTH);
  return result.buffer;
}

export async function decryptVault(buffer: ArrayBuffer, passphrase: string): Promise<string> {
  const data = new Uint8Array(buffer);
  const minLength = SALT_LENGTH + IV_LENGTH + GCM_TAG_BITS / 8;
  if (data.byteLength < minLength) {
    throw new Error('File is too small or corrupted');
  }

  const salt = data.slice(0, SALT_LENGTH);
  const iv = data.slice(SALT_LENGTH, SALT_LENGTH + IV_LENGTH);
  const ciphertext = data.slice(SALT_LENGTH + IV_LENGTH);
  const key = await deriveKey(passphrase, salt);

  const plaintext = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv, tagLength: GCM_TAG_BITS },
    key,
    ciphertext
  );

  return new TextDecoder().decode(plaintext);
}
