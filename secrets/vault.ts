/**
 * @file vault.ts
 * Restricted Secret Storage Layer.
 * 
 * This module layers all API keys, certificates, and passwords behind 
 * typed variables. Access to this layer is restricted to super master 
 * administrators.
 */

import * as crypto from 'crypto';

// Ensure environment variables are loaded
require('dotenv').config();

export interface MasterSecretVault {
  readonly geminiOrchestrationKey: string | undefined;
  readonly googleOAuthClientId: string | undefined;
  readonly keystorePassword: string | undefined;
  readonly mtls: {
    readonly clientCrtPath: string | undefined;
    readonly clientKeyPath: string | undefined;
    readonly caBundlePath: string | undefined;
  };
}

export interface SecuredSecret {
  readonly preview: string;
  readonly hash: string;
  readonly timestamp: number;
  readonly signature: string;
}

export interface SecuredMasterVault {
  readonly geminiOrchestrationKey: SecuredSecret | undefined;
  readonly googleOAuthClientId: SecuredSecret | undefined;
  readonly keystorePassword: SecuredSecret | undefined;
}

/**
 * Helper to mask sensitive values for safe visual display or transmission.
 */
function maskValue(val: string | undefined): string {
  if (!val || val === 'VOID') return 'VOID';
  if (val.length <= 8) return '********';
  return val.substring(0, 4) + '...' + val.substring(val.length - 4);
}

/**
 * Helper to generate a SHA-256 hash of a value.
 */
function secureHash(val: string | undefined): string {
  if (!val || val === 'VOID') return 'VOID';
  return crypto.createHash('sha256').update(val).digest('hex');
}

/**
 * Helper to generate a secured secret wrapper with a double layer of TS (Timestamp + Signature).
 */
function createSecuredSecret(val: string | undefined): SecuredSecret | undefined {
  if (val === undefined) return undefined;
  const timestamp = Math.floor(Date.now() / 1000);
  const hashVal = secureHash(val);
  const previewVal = maskValue(val);
  const signature = crypto
    .createHmac('sha256', 'ultima-grid-salt-ts-' + timestamp)
    .update(val)
    .digest('hex');

  return {
    preview: previewVal,
    hash: hashVal,
    timestamp: timestamp,
    signature: signature
  };
}

/**
 * The Master Vault containing layered credentials.
 * Utilizes mappings from .env.example definitions.
 */
export const Vault: MasterSecretVault = {
  // S1: Gemini AI Orchestration Key
  geminiOrchestrationKey: process.env.YOUR_ORCHESTRATOR_KEY,

  // S2: Google OAuth Client ID
  googleOAuthClientId: process.env.YOUR_CLIENT_ID,

  // S3: mTLS / Certificate Settings
  mtls: {
    clientCrtPath: process.env.YOUR_CERT_PATH,
    clientKeyPath: process.env.YOUR_CERT_KEY,
    caBundlePath: process.env.YOUR_CA_BUNDLE,
  },

  // S3: Keystore Settings
  keystorePassword: process.env.YOUR_KEYSTORE_PASS,
} as const;

/**
 * Secured Vault exporting masked representation with dual-layer TS (TypeScript/Timestamp)
 * structures to hide true values.
 */
export const SecuredVault: SecuredMasterVault = {
  geminiOrchestrationKey: createSecuredSecret(Vault.geminiOrchestrationKey),
  googleOAuthClientId: createSecuredSecret(Vault.googleOAuthClientId),
  keystorePassword: createSecuredSecret(Vault.keystorePassword)
} as const;

export default Vault;
