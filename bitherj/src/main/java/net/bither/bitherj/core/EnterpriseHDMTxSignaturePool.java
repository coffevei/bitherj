/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.bitherj.core;

import net.bither.bitherj.crypto.ECKey;
import net.bither.bitherj.crypto.TransactionSignature;
import net.bither.bitherj.script.ScriptBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by songchenwen on 15/6/3.
 */
public class EnterpriseHDMTxSignaturePool {
    private int threshold;
    private Tx tx;
    private List<byte[]> pubs;
    private byte[] multisigProgram;

    private HashMap<Integer, ArrayList<TransactionSignature>> signatures;

    public EnterpriseHDMTxSignaturePool(Tx tx, int threshold, List<byte[]> pubs) {
        assert !tx.isSigned();
        assert pubs.size() >= threshold;
        this.tx = tx;
        this.threshold = threshold;
        this.pubs = pubs;
        signatures = new HashMap<Integer, ArrayList<TransactionSignature>>();
        multisigProgram = ScriptBuilder.createMultiSigOutputScript(threshold, pubs).getProgram();
    }

    public boolean addSignature(List<byte[]> sigs) {
        if (sigs.size() != tx.getIns().size()) {
            return false;
        }
        ArrayList<TransactionSignature> txSigs = new ArrayList<TransactionSignature>();
        int pubIndex = -1;
        for (int i = 0;
             i < tx.getIns().size();
             i++) {
            TransactionSignature txSig = new TransactionSignature(ECKey.ECDSASignature
                    .decodeFromDER(sigs.get(i)), TransactionSignature.SigHash.ALL, false);
            if (i == 0) {
                byte[] pub = recoverPub(txSig, unsignedHashes().get(i));
                if (pub == null) {
                    break;
                }
                pubIndex = pubs.indexOf(pub);
                if (pubIndex < 0) {
                    break;
                }
            }
            txSigs.add(txSig);
        }
        if (pubIndex < 0) {
            return false;
        }
        signatures.put(Integer.valueOf(pubIndex), txSigs);
        return true;
    }

    public Tx sign() {
        assert satisfied();
        ArrayList<byte[]> txSigs = new ArrayList<byte[]>();

        for (int inputIndex = 0;
             inputIndex < tx.getIns().size();
             inputIndex++) {

            ArrayList<TransactionSignature> inputSigs = new ArrayList<TransactionSignature>();

            for (Integer pubIndex : signaturePubIndexes()) {
                if (signatures.containsKey(pubIndex)) {
                    inputSigs.add(signatures.get(pubIndex).get(inputIndex));
                }
            }

            txSigs.add(ScriptBuilder.createP2SHMultiSigInputScript(inputSigs, multisigProgram)
                    .getProgram());
        }

        tx.signWithSignatures(txSigs);

        if (tx.verifySignatures()) {
            return tx;
        } else {
            return null;
        }
    }

    public List<byte[]> unsignedHashes() {
        return tx.getUnsignedInHashesForHDM(multisigProgram);
    }

    public boolean satisfied() {
        return signatureCount() >= threshold();
    }

    public int signatureCount() {
        return signatures.values().size();
    }

    public List<Integer> signaturePubIndexes() {
        ArrayList<Integer> indexes = new ArrayList<Integer>();
        indexes.addAll(signatures.keySet());
        indexes.sort(null);
        return indexes;
    }

    public int threshold() {
        return threshold;
    }

    private byte[] recoverPub(TransactionSignature signature, byte[] hash) {
        // TODO EnterpriseHDMTxSignaturePool recoverPub
        return null;
    }
}