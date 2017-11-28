/*
 *
 * Copyright (c) 2013 - 2017 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.security.pkcs11.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.common.util.LogUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.exception.P11TokenException;
import org.xipki.security.exception.XiSecurityException;
import org.xipki.security.pkcs11.P11CryptService;
import org.xipki.security.pkcs11.P11CryptServiceFactory;
import org.xipki.security.pkcs11.P11Identity;
import org.xipki.security.pkcs11.P11Module;
import org.xipki.security.pkcs11.P11ObjectIdentifier;
import org.xipki.security.pkcs11.P11Slot;
import org.xipki.security.pkcs11.P11SlotIdentifier;

/**
 * construction of alias:
 * <ul>
 *   <li><code>&lt;module name>#slotid-&lt;slot id>#keyid-&lt;key id></code></li>
 *   <li><code>&lt;module name>#slotid-&lt;slot id>#keylabel-&lt;key label></code></li>
 *   <li><code>&lt;module name>#slotindex-&lt;slot index>#keyid-&lt;key id></code></li>
 *   <li><code>&lt;module name>#slotindex-&lt;slot index>#keylabel-&lt;key label></code></li>
 * </ul>
 *
 * @author Lijun Liao
 * @since 2.0.0
 */

public class XipkiKeyStoreSpi extends KeyStoreSpi {

    private static final Logger LOG = LoggerFactory.getLogger(XipkiKeyStoreSpi.class);

    private static class MyEnumeration<E> implements Enumeration<E> {

        private Iterator<E> iter;

        MyEnumeration(final Iterator<E> iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasMoreElements() {
            return iter.hasNext();
        }

        @Override
        public E nextElement() {
            return iter.next();
        }

    } // class MyEnumeration

    private static class KeyCertEntry {

        private PrivateKey key;

        private Certificate[] chain;

        KeyCertEntry(final PrivateKey key, final Certificate[] chain) {
            this.key = ParamUtil.requireNonNull("key", key);
            this.chain = ParamUtil.requireNonNull("chain", chain);
            if (chain.length < 1) {
                throw new IllegalArgumentException("chain does not contain any certificate");
            }
        }

        PrivateKey getKey() {
            return key;
        }

        Certificate[] getCertificateChain() {
            return Arrays.copyOf(chain, chain.length);
        }

        Certificate getCertificate() {
            return chain[0];
        }

    } // class KeyCertEntry

    private static P11CryptServiceFactory p11CryptServiceFactory;

    private Date creationDate;

    private Map<String, KeyCertEntry> keyCerts = new HashMap<>();

    public static void setP11CryptServiceFactory(final P11CryptServiceFactory service) {
        p11CryptServiceFactory = service;
    }

    @Override
    public void engineLoad(final InputStream stream, final char[] password)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        this.creationDate = new Date();

        Set<String> moduleNames = p11CryptServiceFactory.moduleNames();
        for (String moduleName : moduleNames) {
            try {
                engineLoad(moduleName);
            } catch (XiSecurityException | P11TokenException ex) {
                LogUtil.error(LOG, ex, "could not load PKCS#11 module " + moduleName);
            }
        }

        if (LOG.isErrorEnabled()) {
            LOG.info("loaded key entries {}", keyCerts.keySet());
        }
    }

    private void engineLoad(final String moduleName) throws P11TokenException, XiSecurityException {
        P11CryptService p11Service = p11CryptServiceFactory.getP11CryptService(moduleName);
        P11Module module = p11Service.module();
        List<P11SlotIdentifier> slotIds = module.slotIdentifiers();

        for (P11SlotIdentifier slotId: slotIds) {
            P11Slot slot = module.getSlot(slotId);
            Set<P11ObjectIdentifier> identityIds = slot.identityIdentifiers();
            for (P11ObjectIdentifier objId : identityIds) {
                P11Identity identity = slot.getIdentity(objId);
                X509Certificate[] chain = identity.certificateChain();
                if (chain == null || chain.length == 0) {
                    continue;
                }

                P11PrivateKey key = new P11PrivateKey(p11Service, identity.identityId());
                KeyCertEntry keyCertEntry = new KeyCertEntry(key, chain);
                keyCerts.put(moduleName + "#slotid-" + slotId.id() + "#keyid-"
                        + objId.idHex(), keyCertEntry);
                keyCerts.put(moduleName + "#slotid-" + slotId.id() + "#keylabel-"
                            + objId.label(), keyCertEntry);
                keyCerts.put(moduleName + "#slotindex-" + slotId.index() + "#keyid-"
                            + objId.idHex(), keyCertEntry);
                keyCerts.put(moduleName + "#slotindex-" + slotId.index() + "#keylabel-"
                            + objId.label(), keyCertEntry);
            }
        }
    } // method engineLoad

    @Override
    public void engineStore(final OutputStream stream, final char[] password)
            throws IOException, NoSuchAlgorithmException, CertificateException {
    }

    @Override
    public Key engineGetKey(final String alias, final char[] password)
            throws NoSuchAlgorithmException, UnrecoverableKeyException {
        if (!keyCerts.containsKey(alias)) {
            return null;
        }

        return keyCerts.get(alias).getKey();
    }

    @Override
    public Certificate[] engineGetCertificateChain(final String alias) {
        if (!keyCerts.containsKey(alias)) {
            return null;
        }

        return keyCerts.get(alias).getCertificateChain();
    }

    @Override
    public Certificate engineGetCertificate(final String alias) {
        if (!keyCerts.containsKey(alias)) {
            return null;
        }

        return keyCerts.get(alias).getCertificate();
    }

    @Override
    public Date engineGetCreationDate(final String alias) {
        if (!keyCerts.containsKey(alias)) {
            return null;
        }
        return creationDate;
    }

    @Override
    public void engineSetKeyEntry(final String alias, final Key key, final char[] password,
            final Certificate[] chain) throws KeyStoreException {
        throw new KeyStoreException("keystore is read only");
    }

    @Override
    public void engineSetKeyEntry(final String alias, final byte[] key, final Certificate[] chain)
            throws KeyStoreException {
        throw new KeyStoreException("keystore is read only");
    }

    @Override
    public void engineSetCertificateEntry(final String alias, final Certificate cert)
            throws KeyStoreException {
        throw new KeyStoreException("keystore is read only");
    }

    @Override
    public void engineDeleteEntry(final String alias) throws KeyStoreException {
        throw new KeyStoreException("keystore is read only");
    }

    @Override
    public Enumeration<String> engineAliases() {
        return new MyEnumeration<>(keyCerts.keySet().iterator());
    }

    @Override
    public boolean engineContainsAlias(final String alias) {
        return keyCerts.containsKey(alias);
    }

    @Override
    public int engineSize() {
        return keyCerts.size();
    }

    @Override
    public boolean engineIsKeyEntry(final String alias) {
        if (!keyCerts.containsKey(alias)) {
            return false;
        }

        return keyCerts.get(alias).key != null;
    }

    @Override
    public boolean engineIsCertificateEntry(final String alias) {
        if (!keyCerts.containsKey(alias)) {
            return false;
        }

        return keyCerts.get(alias).key == null;
    }

    @Override
    public String engineGetCertificateAlias(final Certificate cert) {
        for (String alias : keyCerts.keySet()) {
            if (keyCerts.get(alias).getCertificate().equals(cert)) {
                return alias;
            }
        }

        return null;
    }

}
