package icons;

import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;

/**
 * Created by pc on 2016/4/6.
 */
public class PluginIcons {
	public static final Icon PackerIcon = load("/icons/sun.png");

	private static Icon load(String path) {
		return IconLoader.getIcon(path, PluginIcons.class);
	}
}
