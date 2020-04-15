package org.ergoplatform.appkit;

import special.sigma.GroupElement;

import java.math.BigInteger;

/**
 * This interface is used to configure and build a new {@link ErgoProver prover}.
 */
public interface ErgoProverBuilder {
    /**
     * Configure this builder to use the given seed when building a new prover.
     *
     * @param mnemonicPhrase secret seed phrase to be used in prover for generating proofs.
     * @param mnemonicPass   password to protect secret seed phrase.
     */
    ErgoProverBuilder withMnemonic(SecretString mnemonicPhrase, SecretString mnemonicPass);

    /**
     * Configure this builder to use the given mnemonic when building a new prover.
     *
     * @param mnemonic {@link Mnemonic} instance containing secret seed phrase to be used in prover for
     *                 generating proofs.
     */
    ErgoProverBuilder withMnemonic(Mnemonic mnemonic);

    /**
     * Configure this builder to use the given {@link SecretStorage} when building a new prover.
     *
     * @param storage {@link SecretStorage} instance containing encrypted secret seed phrase to be used in
     *                prover for generating proofs.
     */
    ErgoProverBuilder withSecretStorage(SecretStorage storage);

    /**
     * ProveDHTuple is of the form (g, h, u, v) with secret x (and unknown y), where:
     *   h = g^y
     *   u = g^x
     *   v = g^xy
     *
     *   NOTE: We can swap x, y and obtain another tuple (g, u, h, v) with secret y (and unknown x).
     *
     * @param g {@Link GroupElement} instance defining g
     * @param h {@Link GroupElement} instance defining h
     * @param u {@Link GroupElement} instance defining u
     * @param v {@Link GroupElement} instance defining v
     * @param x {@Link BigInteger} instance defining x
     *
     * @see
     * <a href="https://github.com/ScorexFoundation/sigmastate-interpreter/blob/b3695bdb785c9b3a94545ffea506358ee3f8ed3d/sigmastate/src/test/scala/sigmastate/utxo/examples/DHTupleExampleSpecification.scala#L28">example</a>
     *
     */
    ErgoProverBuilder withDHTData(GroupElement g, GroupElement h, GroupElement u, GroupElement v, BigInteger x);

    ErgoProverBuilder withDLogSecret(BigInteger x);

    /**
     * Builds a new prover using provided configuration.
     */
    ErgoProver build();
}

