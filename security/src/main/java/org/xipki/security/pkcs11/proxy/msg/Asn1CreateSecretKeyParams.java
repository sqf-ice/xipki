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

package org.xipki.security.pkcs11.proxy.msg;

import java.io.IOException;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.util.Arrays;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.exception.BadAsn1ObjectException;
import org.xipki.security.pkcs11.P11NewKeyControl;
import org.xipki.security.pkcs11.P11SlotIdentifier;

/**
 *
 * <pre>
 * CreateSecretKeyParams ::= SEQUENCE {
 *     slotId               P11SlotIdentifier,
 *     label                UTF8STRING,
 *     control              NewKeyControl,
 *     keyType              INTEGER,
 *     keyValue             OCTET STRING}
 * </pre>
 *
 * @author Lijun Liao
 * @since 2.0.0
 */

// CHECKSTYLE:SKIP
public class Asn1CreateSecretKeyParams extends ASN1Object {

    private final P11SlotIdentifier slotId;

    private final String label;

    private final P11NewKeyControl control;

    private final long keyType;

    private final byte[] keyValue;

    public Asn1CreateSecretKeyParams(final P11SlotIdentifier slotId, final String label,
            final P11NewKeyControl control, final long keyType, final byte[] keyValue) {
        this.slotId = ParamUtil.requireNonNull("slotId", slotId);
        this.label = ParamUtil.requireNonBlank("label", label);
        this.control = ParamUtil.requireNonNull("control", control);
        this.keyType = keyType;
        this.keyValue = ParamUtil.requireNonNull("keyValue", keyValue);
    }

    private Asn1CreateSecretKeyParams(final ASN1Sequence seq) throws BadAsn1ObjectException {
        Asn1Util.requireRange(seq, 5, 5);
        int idx = 0;
        slotId = Asn1P11SlotIdentifier.getInstance(seq.getObjectAt(idx++)).slotId();
        label = Asn1Util.getUtf8String(seq.getObjectAt(idx++));
        control = Asn1NewKeyControl.getInstance(seq.getObjectAt(idx++)).control();
        keyType = Asn1Util.getInteger(seq.getObjectAt(idx++)).longValue();
        keyValue = ASN1OctetString.getInstance(seq.getObjectAt(idx++)).getOctets();
    }

    public static Asn1CreateSecretKeyParams getInstance(final Object obj)
            throws BadAsn1ObjectException {
        if (obj == null || obj instanceof Asn1CreateSecretKeyParams) {
            return (Asn1CreateSecretKeyParams) obj;
        }

        try {
            if (obj instanceof ASN1Sequence) {
                return new Asn1CreateSecretKeyParams((ASN1Sequence) obj);
            } else if (obj instanceof byte[]) {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
            } else {
                throw new BadAsn1ObjectException("unknown object: " + obj.getClass().getName());
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new BadAsn1ObjectException("unable to parse encoded object: " + ex.getMessage(),
                    ex);
        }
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        vector.add(new Asn1P11SlotIdentifier(slotId));
        vector.add(new DERUTF8String(label));
        vector.add(new ASN1Integer(keyType));
        vector.add(new DEROctetString(keyValue));
        return new DERSequence(vector);
    }

    public P11SlotIdentifier slotId() {
        return slotId;
    }

    public String label() {
        return label;
    }

    public P11NewKeyControl control() {
        return control;
    }

    public long keyType() {
        return keyType;
    }

    public byte[] keyValue() {
        return Arrays.copyOf(keyValue, keyValue.length);
    }

}
