-- Clear all existing API tokens: their BCrypt hashes are incompatible with the
-- new SHA-256 hashing introduced in this version. Users must create new tokens.
DELETE FROM api_tokens;