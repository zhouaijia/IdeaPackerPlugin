package com.aijia.plugin.keyUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import sun.misc.BASE64Encoder;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.util.DerValue;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

/**
 * Created by pc on 2016/4/7.
 */
public class SignedJarBuilder {
	private static final String DIGEST_ALGORITHM = "SHA1";
	private static final String DIGEST_ATTR = "SHA1-Digest";
	private static final String DIGEST_MANIFEST_ATTR = "SHA1-Digest-Manifest";
	private JarOutputStream mOutputJar;
	private PrivateKey mKey;
	private X509Certificate mCertificate;
	private Manifest mManifest;
	private BASE64Encoder mBase64Encoder;
	private MessageDigest mMessageDigest;
	private byte[] mBuffer = new byte[4096];

	public SignedJarBuilder(OutputStream out, PrivateKey key, X509Certificate certificate) throws IOException, NoSuchAlgorithmException {
		this.mOutputJar = new JarOutputStream(out);
		this.mOutputJar.setLevel(9);
		this.mKey = key;
		this.mCertificate = certificate;
		if(this.mKey != null && this.mCertificate != null) {
			this.mManifest = new Manifest();
			Attributes main = this.mManifest.getMainAttributes();
			main.putValue("Manifest-Version", "1.0");
			main.putValue("Created-By", "1.0 (Android)");
			this.mBase64Encoder = new BASE64Encoder();
			this.mMessageDigest = MessageDigest.getInstance("SHA1");
		}

	}

	public void writeFile(File inputFile, String jarPath) throws IOException {
		FileInputStream fis = new FileInputStream(inputFile);

		try {
			JarEntry entry = new JarEntry(jarPath);
			entry.setTime(inputFile.lastModified());
			this.writeEntry(fis, entry);
		} finally {
			fis.close();
		}

	}

	public void writeZip(InputStream input, SignedJarBuilder.IZipEntryFilter filter) throws IOException {
		ZipInputStream zis = new ZipInputStream(input);

		ZipEntry entry;
		try {
			while((entry = zis.getNextEntry()) != null) {
				String name = entry.getName();
				if(!entry.isDirectory() && !name.startsWith("META-INF/") && (filter == null || filter.checkEntry(name))) {
					JarEntry newEntry;
					if(entry.getMethod() == 0) {
						newEntry = new JarEntry(entry);
					} else {
						newEntry = new JarEntry(name);
					}

					this.writeEntry(zis, newEntry);
					zis.closeEntry();
				}
			}
		} finally {
			zis.close();
		}

	}

	public void close() throws IOException, GeneralSecurityException {
		if(this.mManifest != null) {
			this.mOutputJar.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
			this.mManifest.write(this.mOutputJar);
			Signature signature = Signature.getInstance("SHA1with" + this.mKey.getAlgorithm());
			signature.initSign(this.mKey);
			this.mOutputJar.putNextEntry(new JarEntry("META-INF/CERT.SF"));
			this.writeSignatureFile(new SignedJarBuilder.SignatureOutputStream(this.mOutputJar, signature));
			this.mOutputJar.putNextEntry(new JarEntry("META-INF/CERT." + this.mKey.getAlgorithm()));
			this.writeSignatureBlock(signature, this.mCertificate, this.mKey);
		}

		this.mOutputJar.close();
	}

	private void writeEntry(InputStream input, JarEntry entry) throws IOException {
		this.mOutputJar.putNextEntry(entry);

		int count;
		while((count = input.read(this.mBuffer)) != -1) {
			this.mOutputJar.write(this.mBuffer, 0, count);
			if(this.mMessageDigest != null) {
				this.mMessageDigest.update(this.mBuffer, 0, count);
			}
		}

		this.mOutputJar.closeEntry();
		if(this.mManifest != null) {
			Attributes attr = this.mManifest.getAttributes(entry.getName());
			if(attr == null) {
				attr = new Attributes();
				this.mManifest.getEntries().put(entry.getName(), attr);
			}

			attr.putValue("SHA1-Digest", this.mBase64Encoder.encode(this.mMessageDigest.digest()));
		}

	}

	private void writeSignatureFile(OutputStream out) throws IOException, GeneralSecurityException {
		Manifest sf = new Manifest();
		Attributes main = sf.getMainAttributes();
		main.putValue("Signature-Version", "1.0");
		main.putValue("Created-By", "1.0 (Android)");
		BASE64Encoder base64 = new BASE64Encoder();
		MessageDigest md = MessageDigest.getInstance("SHA1");
		PrintStream print = new PrintStream(new DigestOutputStream(new ByteArrayOutputStream(), md), true, "UTF-8");
		this.mManifest.write(print);
		print.flush();
		main.putValue("SHA1-Digest-Manifest", base64.encode(md.digest()));
		Map entries = this.mManifest.getEntries();
		Iterator i$ = entries.entrySet().iterator();

		while(i$.hasNext()) {
			Map.Entry entry = (Map.Entry)i$.next();
			print.print("Name: " + (String)entry.getKey() + "\r\n");
			Iterator sfAttr = ((Attributes)entry.getValue()).entrySet().iterator();

			while(sfAttr.hasNext()) {
				Map.Entry att = (Map.Entry)sfAttr.next();
				print.print(att.getKey() + ": " + att.getValue() + "\r\n");
			}

			print.print("\r\n");
			print.flush();
			Attributes sfAttr1 = new Attributes();
			sfAttr1.putValue("SHA1-Digest", base64.encode(md.digest()));
			sf.getEntries().put(entry.getKey()+"", sfAttr1);
		}

		sf.write(out);
	}

	private void writeSignatureBlock(Signature signature, X509Certificate publicKey, PrivateKey privateKey) throws IOException, GeneralSecurityException {
		SignerInfo signerInfo = new SignerInfo(new X500Name(publicKey.getIssuerX500Principal().getName()), publicKey.getSerialNumber(), AlgorithmId.get("SHA1"), AlgorithmId.get(privateKey.getAlgorithm()), signature.sign());
		PKCS7 pkcs7 = new PKCS7(new AlgorithmId[]{AlgorithmId.get("SHA1")}, new ContentInfo(ContentInfo.DATA_OID, (DerValue)null), new X509Certificate[]{publicKey}, new SignerInfo[]{signerInfo});
		pkcs7.encodeSignedData(this.mOutputJar);
	}

	public interface IZipEntryFilter {
		boolean checkEntry(String var1);
	}

	private static class SignatureOutputStream extends FilterOutputStream {
		private Signature mSignature;

		public SignatureOutputStream(OutputStream out, Signature sig) {
			super(out);
			this.mSignature = sig;
		}

		public void write(int b) throws IOException {
			try {
				this.mSignature.update((byte)b);
			} catch (SignatureException var3) {
				throw new IOException("SignatureException: " + var3);
			}

			super.write(b);
		}

		public void write(byte[] b, int off, int len) throws IOException {
			try {
				this.mSignature.update(b, off, len);
			} catch (SignatureException var5) {
				throw new IOException("SignatureException: " + var5);
			}

			super.write(b, off, len);
		}
	}
}
