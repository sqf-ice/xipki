/*
 * Copyright (c) 2014 Lijun Liao
 *
 * TO-BE-DEFINE
 *
 */

package org.xipki.security.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.cmp.PKIFreeText;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962NamedCurves;

/**
 * @author Lijun Liao
 */

public class IoCertUtil
{
    public static final Map<Integer, String> statusTextMap = new HashMap<>();
    public static final String[] failureInfoTexts = new String[]
    {
        "incorrectData", "wrongAuthority", "badDataFormat", "badCertId", // 0 - 3
        "badTime", "badRequest", "badMessageCheck", "badAlg", // 4 - 7
        "unacceptedPolicy", "timeNotAvailable", "badRecipientNonce", "wrongIntegrity", // 8 - 11
        "certConfirmed", "certRevoked", "badPOP", "missingTimeStamp", // 12 - 15
        "notAuthorized", "unsupportedVersion", "transactionIdInUse", "signerNotTrusted", // 16 - 19
        "badCertTemplate", "badSenderNonce", "addInfoNotAvailable", "unacceptedExtension", // 20 - 23
        "-", "-", "-", "-", // 24 -27
        "-", "duplicateCertReq", "systemFailure", "systemUnavail"}; // 28 - 31

    static
    {
        statusTextMap.put(-2, "xipki_noAnswer");
        statusTextMap.put(-1, "xipki_responseError");
        statusTextMap.put(PKIStatus.GRANTED, "accepted");
        statusTextMap.put(PKIStatus.GRANTED_WITH_MODS, "grantedWithMods");
        statusTextMap.put(PKIStatus.REJECTION, "rejection");
        statusTextMap.put(PKIStatus.WAITING, "waiting");
        statusTextMap.put(PKIStatus.REVOCATION_WARNING, "revocationWarning");
        statusTextMap.put(PKIStatus.REVOCATION_NOTIFICATION, "revocationNotification");
        statusTextMap.put(PKIStatus.KEY_UPDATE_WARNING, "keyUpdateWarning");
    }

    public static String getCommonName(X500Name name)
    {
        RDN[] rdns = name.getRDNs(ObjectIdentifiers.DN_CN);
        if(rdns != null && rdns.length > 0)
        {
            return IETFUtils.valueToString(rdns[0].getFirst().getValue());
        }
        return null;
    }

    public static X500Name reverse(X500Name name)
    {
        RDN[] orig = name.getRDNs();
        int n = orig.length;
        RDN[] _new = new RDN[n];
        for(int i = 0; i < n; i++)
        {
            _new[i] = orig[n - 1 - i];
        }
        return new X500Name(_new);
    }

    public static X500Name sortX509Name(X500Name name)
    {
        return sortX500Name(name, false);
    }

    public static X500Name backwardSortX509Name(X500Name name)
    {
        return sortX500Name(name, true);
    }

    private static X500Name sortX500Name(X500Name name, boolean backwards)
    {
        RDN[] requstedRDNs = name.getRDNs();

        List<RDN> rdns = new LinkedList<>();

        List<ASN1ObjectIdentifier> sortedDNs = backwards ?
                ObjectIdentifiers.getBackwardDNs() : ObjectIdentifiers.getForwardDNs();
        int size = sortedDNs.size();
        for(int i = 0; i < size; i++)
        {
            ASN1ObjectIdentifier type = sortedDNs.get(i);
            RDN[] thisRDNs = getRDNs(requstedRDNs, type);
            int n = thisRDNs == null ? 0 : thisRDNs.length;
            if(n == 0)
            {
                continue;
            }

            for(RDN thisRDN : thisRDNs)
            {
                String text = IETFUtils.valueToString(thisRDN.getFirst().getValue());
                rdns.add(createSubjectRDN(text, type));
            }
        }

        return new X500Name(rdns.toArray(new RDN[0]));
    }

    private static RDN[] getRDNs(RDN[] rdns, ASN1ObjectIdentifier type)
    {
        List<RDN> ret = new ArrayList<>(1);
        for(int i = 0; i < rdns.length; i++)
        {
            RDN rdn = rdns[i];
            if(rdn.getFirst().getType().equals(type))
            {
                ret.add(rdn);
            }
        }

        if(ret.isEmpty())
        {
            return null;
        }
        else
        {
            return ret.toArray(new RDN[0]);
        }
    }

    private static RDN createSubjectRDN(String text, ASN1ObjectIdentifier type)
    {
        ASN1Encodable dnValue;
        if(ObjectIdentifiers.DN_SERIALNUMBER.equals(type) ||
           ObjectIdentifiers.DN_C.equals(type))
        {
            dnValue = new DERPrintableString(text);
        }
        else
        {
            dnValue = new DERUTF8String(text);
        }

        RDN rdn = new RDN(type, dnValue);

        return rdn;
    }

    public static byte[] read(String fileName)
    throws IOException
    {
        return read(new File(expandFilepath(fileName)));
    }

    public static byte[] read(File file)
    throws IOException
    {
        return read(new FileInputStream(expandFilepath(file)));
    }

    public static byte[] read(InputStream in)
    throws IOException
    {
        try
        {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            int readed = 0;
            byte[] buffer = new byte[2048];
            while ((readed = in.read(buffer)) != -1)
            {
                bout.write(buffer, 0, readed);
            }

            return bout.toByteArray();
        } finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                } catch (IOException e)
                {
                }
            }
        }
    }

    public static void save(String fileName, byte[] encoded)
    throws IOException
    {
        save(new File(expandFilepath(fileName)), encoded);
    }

    public static void save(File file, byte[] encoded)
    throws IOException
    {
           file = expandFilepath(file);

        File parent = file.getParentFile();
        if (parent != null && parent.exists() == false)
        {
            parent.mkdirs();
        }

        FileOutputStream out = new FileOutputStream(file);
        try
        {
            out.write(encoded);
        } finally
        {
            out.close();
        }
    }

    private static CertificateFactory certFact;
    private static Object certFactLock = new Object();

    public static X509Certificate parseCert(String fileName)
    throws IOException, CertificateException
    {
        return parseCert(new File(expandFilepath(fileName)));
    }

    public static X509Certificate parseCert(File file)
    throws IOException, CertificateException
    {
        FileInputStream in = new FileInputStream(expandFilepath(file));
        try
        {
            return parseCert(in);
        }finally
        {
            in.close();
        }
    }

    public static X509Certificate parseCert(byte[] certBytes)
    throws IOException, CertificateException
    {
        return parseCert(new ByteArrayInputStream(certBytes));
    }

    public static X509Certificate parseCert(InputStream certStream)
    throws IOException, CertificateException
    {
        synchronized (certFactLock)
        {
            if (certFact == null)
            {
                try
                {
                    certFact = CertificateFactory.getInstance("X.509", "BC");
                } catch (NoSuchProviderException e)
                {
                    throw new IOException("NoSuchProviderException: " + e.getMessage());
                }
            }
        }

        return (X509Certificate) certFact.generateCertificate(certStream);
    }

    public static X509CRL parseCRL(String f)
    throws IOException, CertificateException, CRLException
    {
        return parseCRL(new FileInputStream(expandFilepath(f)));
    }

    public static X509CRL parseCRL(InputStream crlStream)
    throws IOException, CertificateException, CRLException
    {
        try
        {
            if(certFact == null)
            {
                certFact = CertificateFactory.getInstance("X.509", "BC");
            }
            return (X509CRL) certFact.generateCRL(crlStream);
        } catch (NoSuchProviderException e)
        {
            throw new IOException("NoSuchProviderException: " + e.getMessage());
        }
    }

    public static String canonicalizeName(X500Principal name)
    {
        return canonicalizeName(X500Name.getInstance(name.getEncoded()));
    }

    public static String canonicalizeName(X500Name name)
    {
        return RFC4519Style.INSTANCE.toString(name);
    }

    /**
     * First canonicalized the name, and then compute the SHA-1 finger-print over the
     * canonicalized subject string.
     */
    public static String sha1sum_canonicalized_name(X500Principal prin)
    {
        X500Name x500Name = X500Name.getInstance(prin.getEncoded());
        return sha1sum_canonicalized_name(x500Name);
    }

    public static String sha1sum_canonicalized_name(X500Name name)
    {
        ASN1ObjectIdentifier[] _types = name.getAttributeTypes();
        int n = _types.length;
        List<String> types = new ArrayList<>(n);
        for(ASN1ObjectIdentifier type : _types)
        {
            types.add(type.getId());
        }

        Collections.sort(types);

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < n; i++)
        {
            String type = types.get(i);
            if(i > 0)
            {
                sb.append(",");
            }
            sb.append(type).append("=");
            RDN[] rdns = name.getRDNs(new ASN1ObjectIdentifier(type));

            for(int j = 0; j < rdns.length; j++)
            {
                if(j > 0)
                {
                    sb.append(";");
                }
                RDN rdn = rdns[j];
                String textValue = IETFUtils.valueToString(rdn.getFirst().getValue()).toLowerCase();
                sb.append(textValue);
            }
        }

        String canonicalizedName = sb.toString();
        byte[] encoded;
        try
        {
            encoded = canonicalizedName.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            encoded = canonicalizedName.getBytes();
        }
        return sha1sum(encoded);
    }

    public static String sha1sum(byte[] data)
    {
        return HashCalculator.hexHash(HashAlgoType.SHA1, data);
    }

    public static byte[] extractMinimalKeyStore(String keystoreType, byte[] keystoreBytes,
            String keyname, char[] password)
    throws Exception
    {
        KeyStore ks;
        if("JKS".equalsIgnoreCase(keystoreType))
        {
            ks = KeyStore.getInstance(keystoreType);
        }
        else
        {
             ks = KeyStore.getInstance(keystoreType, "BC");
        }
        ks.load(new ByteArrayInputStream(keystoreBytes), password);

        if(keyname == null)
        {
            Enumeration<String> aliases = ks.aliases();
            while(aliases.hasMoreElements())
            {
                String alias = aliases.nextElement();
                if(ks.isKeyEntry(alias))
                {
                    keyname = alias;
                    break;
                }
            }
        }
        else
        {
            if(ks.isKeyEntry(keyname) == false)
            {
                throw new KeyStoreException("unknown key named " + keyname);
            }
        }

        Enumeration<String> aliases = ks.aliases();
        int numAliases = 0;
        while(aliases.hasMoreElements())
        {
            aliases.nextElement();
            numAliases++;
        }

        Certificate[] certs = ks.getCertificateChain(keyname);
        if(numAliases == 1)
        {
            return keystoreBytes;
        }

        PrivateKey key = (PrivateKey) ks.getKey(keyname, password);
        ks = null;

        if("JKS".equalsIgnoreCase(keystoreType))
        {
            ks = KeyStore.getInstance(keystoreType);
        }
        else
        {
             ks = KeyStore.getInstance(keystoreType, "BC");
        }
        ks.load(null, password);
        ks.setKeyEntry(keyname, key, password, certs);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ks.store(bout, password);
        byte[] bytes = bout.toByteArray();
        bout.close();
        return bytes;
    }

    public static X509Certificate[] buildCertPath(X509Certificate cert, Set<? extends Certificate> certs)
    {
        List<X509Certificate> certChain = new LinkedList<>();
        certChain.add(cert);
        if(certs != null && isSelfSigned(cert) == false)
        {
            while(true)
            {
                X509Certificate caCert = getCaCertOf(certChain.get(certChain.size() - 1), certs);
                if(caCert == null)
                {
                    break;
                }
                certChain.add(caCert);
                if(isSelfSigned(caCert))
                {
                    // reaches root self-signed certificate
                    break;
                }
            }
        }

        return certChain.toArray(new X509Certificate[0]);
    }

    public static X509Certificate[] buildCertPath(X509Certificate cert, Certificate[] certs)
    {
        Set<Certificate> setOfCerts = new HashSet<>();
        for(Certificate entry : certs)
        {
            setOfCerts.add(entry);
        }

        return buildCertPath(cert, setOfCerts);
    }

    private static X509Certificate getCaCertOf(X509Certificate cert,
            Set<? extends Certificate> caCerts)
    {
        if(isSelfSigned(cert))
        {
            return null;
        }

        X500Principal issuer = cert.getIssuerX500Principal();

        for(Certificate caCert : caCerts)
        {
            if(caCert instanceof X509Certificate == false)
            {
                continue;
            }

            X509Certificate x509Cert = (X509Certificate) caCert;
            if(issuer.equals(x509Cert.getSubjectX500Principal()) == false)
            {
                continue;
            }

            boolean isCACert = x509Cert.getBasicConstraints() >= 0;
            if(isCACert == false)
            {
                continue;
            }

            try
            {
                cert.verify(x509Cert.getPublicKey());
                return x509Cert;
            } catch (Exception e)
            {
            }
        }

        return null;
    }

    public static String formatPKIStatusInfo(org.bouncycastle.asn1.cmp.PKIStatusInfo pkiStatusInfo)
    {
        int status = pkiStatusInfo.getStatus().intValue();
        int failureInfo = pkiStatusInfo.getFailInfo().intValue();
        PKIFreeText text = pkiStatusInfo.getStatusString();
        String statusMessage = text == null ? null : text.getStringAt(0).getString();

        return IoCertUtil.formatPKIStatusInfo(status, failureInfo, statusMessage);
    }

    public static String formatPKIStatusInfo(int status, int failureInfo, String statusMessage)
    {
        StringBuilder sb = new StringBuilder("PKIStatusInfo {");
        sb.append("status = ");
        sb.append(status);
        sb.append(" (").append(statusTextMap.get(status)).append("), ");
        sb.append("failureInfo = ");
        sb.append(failureInfo).append(" (").append(getFailureInfoText(failureInfo)).append("), ");
        sb.append("statusMessage = ").append(statusMessage);
        sb.append("}");
        return sb.toString();
    }

    public static String getFailureInfoText(int failureInfo)
    {
        BigInteger b = BigInteger.valueOf(failureInfo);
        final int n = Math.min(b.bitLength(), failureInfoTexts.length);

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < n; i++)
        {
            if(b.testBit(i))
            {
                sb.append(", ").append(failureInfoTexts[i]);
            }
        }

        return sb.length() < 3 ? "" : sb.substring(2);
    }

    public static boolean isSelfSigned(X509Certificate cert)
    {
        return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
    }

    public static byte[] leftmost(byte[] bytes, int bitCount)
    {
        int byteLenKey = (bitCount + 7)/8;

        if (bitCount >= (bytes.length << 3))
        {
            return bytes;
        }

        byte[] truncatedBytes = new byte[byteLenKey];
        System.arraycopy(bytes, 0, truncatedBytes, 0, byteLenKey);

        if (bitCount%8 > 0) // shift the bits to the right
        {
            int shiftBits = 8-(bitCount%8);

            for(int i = byteLenKey - 1; i > 0; i--)
            {
                truncatedBytes[i] = (byte) (
                        (byte2int(truncatedBytes[i]) >>> shiftBits) |
                        ((byte2int(truncatedBytes[i- 1]) << (8 - shiftBits)) & 0xFF));
            }
            truncatedBytes[0] = (byte)(byte2int(truncatedBytes[0]) >>> shiftBits);
        }

        return truncatedBytes;
    }

    private static int byte2int(byte b)
    {
        return b >= 0 ? b : 256 + b;
    }

    public static String getHostAddress()
    throws SocketException
    {
        List<String> addresses = new LinkedList<>();

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while(interfaces.hasMoreElements())
        {
            NetworkInterface n = (NetworkInterface) interfaces.nextElement();
            Enumeration<InetAddress> ee = n.getInetAddresses();
            while (ee.hasMoreElements())
            {
                InetAddress i = (InetAddress) ee.nextElement();
                if(i instanceof Inet4Address)
                {
                    addresses.add(((Inet4Address) i).getHostAddress());
                }
            }
        }

        for(String addr : addresses)
        {
            if(addr.startsWith("192.") == false && addr.startsWith("127.") == false)
            {
                return addr;
            }
        }

        for(String addr : addresses)
        {
            if(addr.startsWith("127.") == false)
            {
                return addr;
            }
        }

        if(addresses.size() > 0)
        {
            return addresses.get(0);
        }
        else
        {
            try
            {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e)
            {
                return "UNKNOWN";
            }
        }
    }

    public static SubjectPublicKeyInfo toRfc3279Style(SubjectPublicKeyInfo publicKeyInfo)
    {
        ASN1ObjectIdentifier algOid = publicKeyInfo.getAlgorithm().getAlgorithm();
        ASN1Encodable keyParameters = publicKeyInfo.getAlgorithm().getParameters();

        if(PKCSObjectIdentifiers.rsaEncryption.equals(algOid))
        {
            if(DERNull.INSTANCE.equals(keyParameters))
            {
                return publicKeyInfo;
            }
            else
            {
                keyParameters = null;
            }
        }

        // Set the parameters field to NULL if not specified
        if(keyParameters == null)
        {
            AlgorithmIdentifier keyAlgId = new AlgorithmIdentifier(algOid, DERNull.INSTANCE);
            publicKeyInfo = new SubjectPublicKeyInfo(keyAlgId, publicKeyInfo.getPublicKeyData().getBytes());
        }
        return publicKeyInfo;
    }

    public static String getCurveName(ASN1ObjectIdentifier curveId)
    {
        String curveName = X962NamedCurves.getName(curveId);

        if (curveName == null)
        {
            curveName = SECNamedCurves.getName(curveId);
        }

        if (curveName == null)
        {
            curveName = TeleTrusTNamedCurves.getName(curveId);
        }

        if (curveName == null)
        {
            curveName = NISTNamedCurves.getName(curveId);
        }

        return curveName;
    }

    public static String expandFilepath(String path)
    {
        if (path.startsWith("~" + File.separator))
        {
            return System.getProperty("user.home") + path.substring(1);
        }
        else
        {
            return path;
        }
    }

    public static File expandFilepath(File file)
    {
        String path = file.getPath();
        String expandedPath = expandFilepath(path);
        if(path.equals(expandedPath) == false)
        {
            file = new File(expandedPath);
        }

        return file;
    }

}
