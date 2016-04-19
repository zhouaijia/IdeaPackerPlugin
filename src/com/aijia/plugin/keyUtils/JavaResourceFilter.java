package com.aijia.plugin.keyUtils;

import com.android.jarutils.SignedJarBuilder;

/**
 * Created by pc on 2016/4/7.
 */
public class JavaResourceFilter implements SignedJarBuilder.IZipEntryFilter {
	public JavaResourceFilter() {
	}

	public boolean checkEntry(String name) {
		String[] segments = name.split("/");
		if(segments.length == 0) {
			return false;
		} else {
			for(int fileName = 0; fileName < segments.length - 1; ++fileName) {
				if(!checkFolderForPackaging(segments[fileName])) {
					return false;
				}
			}

			String var4 = segments[segments.length - 1];
			return checkFileForPackaging(var4);
		}
	}

	public static boolean checkFolderForPackaging(String folderName) {
		return !folderName.equals("CVS") && !folderName.equals(".svn") && !folderName.equals("SCCS") && !folderName.equals("META-INF") && !folderName.startsWith("_");
	}

	public static boolean checkFileForPackaging(String fileName) {
		String[] fileSegments = fileName.split("\\.");
		String fileExt = "";
		if(fileSegments.length > 1) {
			fileExt = fileSegments[fileSegments.length - 1];
		}

		return checkFileForPackaging(fileName, fileExt);
	}

	public static boolean checkFileForPackaging(String fileName, String extension) {
		return fileName.charAt(0) == 46?false:!"aidl".equalsIgnoreCase(extension) && !"java".equalsIgnoreCase(extension) && !"class".equalsIgnoreCase(extension) && !"scc".equalsIgnoreCase(extension) && !"swp".equalsIgnoreCase(extension) && !"package.html".equalsIgnoreCase(fileName) && !"overview.html".equalsIgnoreCase(fileName) && !".cvsignore".equalsIgnoreCase(fileName) && !".DS_Store".equals(fileName) && fileName.charAt(fileName.length() - 1) != 126;
	}
}
