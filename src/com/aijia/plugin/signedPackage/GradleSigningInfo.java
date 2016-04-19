package com.aijia.plugin.signedPackage;

import org.jetbrains.annotations.NotNull;

/**
 * Created by pc on 2016/4/7.
 */
public class GradleSigningInfo {
	public final String keyStoreFilePath;
	public final char[] keyStorePassword;
	public final String keyAlias;
	public final char[] keyPassword;

	public GradleSigningInfo(@NotNull String keyStoreFilePath, @NotNull char[] keyStorePassword, @NotNull String keyAlias, @NotNull char[] keyPassword) {
		this.keyStoreFilePath = keyStoreFilePath;
		this.keyStorePassword = keyStorePassword;
		this.keyAlias = keyAlias;
		this.keyPassword = keyPassword;
	}
}
