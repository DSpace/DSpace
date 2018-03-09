/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.eperson;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For handling digested secrets (such as passwords).
 * Use {@link #PasswordHash(String, byte[], byte[])} to package and manipulate
 * secrets that have already been hashed, and {@link #PasswordHash(String)} for
 * plaintext secrets.  Compare a plaintext candidate to a hashed secret with
 * {@link #matches(String)}.
 *
 * @author mwood
 */
public class PasswordHash
{
    private static final Logger log = LoggerFactory.getLogger(PasswordHash.class);
    private static final ConfigurationService config
            = DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final Charset UTF_8 = Charset.forName("UTF-8"); // Should always succeed:  UTF-8 is required

    private static final String DEFAULT_DIGEST_ALGORITHM = "SHA-512"; // XXX magic
    private static final String ALGORITHM_PROPERTY = "authentication-password.digestAlgorithm";
    private static final int SALT_BYTES = 128/8; // XXX magic we want 128 bits
    private static final int HASH_ROUNDS = 1024; // XXX magic 1024 rounds
    private static final int SEED_BYTES = 64; // XXX magic
    private static final int RESEED_INTERVAL = 100; // XXX magic

    /** A secure random number generator instance. */
    private static SecureRandom rng = null;

    /** How many times has the RNG been called without re-seeding? */
    private static int rngUses;

    private String algorithm;
    private byte[] salt;
    private byte[] hash;

    /** Don't allow empty instances. */
    private PasswordHash() {}

    /**
     * Construct a hash structure from existing data, just for passing around.
     *
     * @param algorithm the digest algorithm used in producing {@code hash}.
     *          If empty, set to null.  Other methods will treat this as unsalted MD5.
     *          If you want salted multi-round MD5, specify "MD5".
     * @param salt the salt hashed with the secret, or null.
     * @param hash the hashed secret.
     */
    public PasswordHash(String algorithm, byte[] salt, byte[] hash)
    {
        if ((null != algorithm) && algorithm.isEmpty())
            this.algorithm = null;
        else
            this.algorithm = algorithm;

        this.salt = salt;

        this.hash = hash;
    }

    /**
     * Convenience:  like {@link #PasswordHash(String, byte[], byte[])} but with
     *          hexadecimal-encoded {@code String}s.
     * @param algorithm the digest algorithm used in producing {@code hash}.
     *          If empty, set to null.  Other methods will treat this as unsalted MD5.
     *          If you want salted multi-round MD5, specify "MD5".
     * @param salt hexadecimal digits encoding the bytes of the salt, or null.
     * @param hash hexadecimal digits encoding the bytes of the hash.
     * @throws DecoderException if salt or hash is not proper hexadecimal.
     */
    public PasswordHash(String algorithm, String salt, String hash)
            throws DecoderException
    {
        if ((null != algorithm) && algorithm.isEmpty())
            this.algorithm = null;
        else
            this.algorithm = algorithm;

        if (null == salt)
            this.salt = null;
        else
            this.salt = Hex.decodeHex(salt.toCharArray());

        if (null == hash)
            this.hash = null;
        else
            this.hash = Hex.decodeHex(hash.toCharArray());
    }

    /**
     * Construct a hash structure from a cleartext password using the configured
     * digest algorithm.
     *
     * @param password the secret to be hashed.
     */
    public PasswordHash(String password)
    {
        // Generate some salt
        salt = generateSalt();

        // What digest algorithm to use?
        algorithm = config.getPropertyAsType(ALGORITHM_PROPERTY, DEFAULT_DIGEST_ALGORITHM);

        // Hash it!
        try {
            hash = digest(salt, algorithm, password);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
            hash = new byte[] { 0 };
        }
    }

    /**
     * Is this the string whose hash I hold?
     *
     * @param secret string to be hashed and compared to this hash.
     * @return true if secret hashes to the value held by this instance.
     */
    public boolean matches(String secret)
    {
        byte[] candidate;
        try {
            candidate = digest(salt, algorithm, secret);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
            return false;
        }
        return Arrays.equals(candidate, hash);
    }

    /**
     * Get the hash.
     *
     * @return the value of hash
     */
    public byte[] getHash()
    {
        return hash;
    }

    /**
     * Get the hash, as a String.
     *
     * @return hash encoded as hexadecimal digits, or null if none.
     */
    public String getHashString()
    {
        if (null != hash)
            return new String(Hex.encodeHex(hash));
        else
            return null;
    }

    /**
     * Get the salt.
     *
     * @return the value of salt
     */
    public byte[] getSalt()
    {
        return salt;
    }

    /**
     * Get the salt, as a String.
     *
     * @return salt encoded as hexadecimal digits, or null if none.
     */
    public String getSaltString()
    {
        if (null != salt)
            return new String(Hex.encodeHex(salt));
        else
            return null;
    }

    /**
     * Get the value of algorithm
     *
     * @return the value of algorithm
     */
    public String getAlgorithm()
    {
        return algorithm;
    }

    /**
     * The digest algorithm used if none is configured.
     * 
     * @return name of the default digest.
     */
    static public String getDefaultAlgorithm()
    {
        return DEFAULT_DIGEST_ALGORITHM;
    }

    /** Generate an array of random bytes. */
    private synchronized byte[] generateSalt()
    {
        // Initialize a random-number generator
        if (null == rng)
        {
            rng = new SecureRandom();
            log.info("Initialized a random number stream using {} provided by {}",
                    rng.getAlgorithm(), rng.getProvider());
            rngUses = 0;
        }

        if (rngUses++ > RESEED_INTERVAL)
        { // re-seed the generator periodically to break up possible patterns
            log.debug("Re-seeding the RNG");
            rng.setSeed(rng.generateSeed(SEED_BYTES));
            rngUses = 0;
        }

        salt = new byte[SALT_BYTES];
        rng.nextBytes(salt);
        return salt;
    }

    /**
     * Generate a salted hash of a string using a given algorithm.
     *
     * @param salt random bytes to salt the hash.
     * @param algorithm name of the digest algorithm to use.  Assume unsalted MD5 if null.
     * @param secret the string to be hashed.  Null is treated as an empty string ("").
     * @return hash bytes.
     * @throws NoSuchAlgorithmException if algorithm is unknown.
     */
    private byte[] digest(byte[] salt, String algorithm, String secret)
            throws NoSuchAlgorithmException
    {
        MessageDigest digester;

        if (null == secret)
            secret = "";

        // Special case:  old unsalted one-trip MD5 hash.
        if (null == algorithm)
        {
            digester = MessageDigest.getInstance("MD5");
            digester.update(secret.getBytes(UTF_8));
            return digester.digest();
        }

        // Set up a digest
        digester =  MessageDigest.getInstance(algorithm);

        // Grind up the salt with the password, yielding a hash
        if (null != salt)
            digester.update(salt);

        digester.update(secret.getBytes(UTF_8)); // Round 0

        for (int round = 1; round < HASH_ROUNDS; round++)
        {
            byte[] lastRound = digester.digest();
            digester.reset();
            digester.update(lastRound);
        }

        return digester.digest();
    }
}
