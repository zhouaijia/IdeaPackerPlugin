package com.aijia.plugin.utils;

import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.util.ResourceBundle;

/**
 * Created by pc on 2016/4/7.
 */
public final class AndroidBundle {
	@NonNls
	private static final String BUNDLE_NAME = "messages.AndroidBundle";
	private static Reference<ResourceBundle> ourBundle;

	private static ResourceBundle getBundle() {
		ResourceBundle bundle = (ResourceBundle) SoftReference.dereference(ourBundle);
		if(bundle == null) {
			bundle = ResourceBundle.getBundle("messages.AndroidBundle");
			ourBundle = new java.lang.ref.SoftReference(bundle);
		}

		return bundle;
	}

	private AndroidBundle() {
	}

	public static String message(
			@NotNull @PropertyKey(
					resourceBundle = "messages.AndroidBundle"
			)String key,
	  @NotNull Object... params) {

		return CommonBundle.message(getBundle(), key, params);
	}
}