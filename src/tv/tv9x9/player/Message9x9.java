package tv.tv9x9.player;

public class Message9x9
{
	/**
	 * System info message from system to 9x9tv
	 * <p>Input: extra data.
	 * <p>Output: nothing.
	 */
	public static String MESSAGE_INFO = "com.edgecore.launcher.tv9x9.message9x9.INFO";

	/**
	 * Upgrade message sent from 9x9tv, to tell system do apk/system upragde
	 * <p>Input: extra data.
	 * <p>Output: nothing.
	 */
	public static String MESSAGE_UPGRADE = "com.edgecore.launcher.tv9x9.message9x9.UPGRADE";

	/**
	 * Clear message sent from 9x9tv, to tell system do cache data clear.
	 * <p>Input: extra data.
	 * <p>Output: nothing.
	 */
	public static String MESSAGE_CLEAR = "com.edgecore.launcher.tv9x9.message9x9.CLEAR";

	/**
	 * Set message sent from 9x9tv, to tell system do net/screen setup
	 * <p>Input: extra data.
	 * <p>Output: nothing.
	 */
	public static String MESSAGE_SET = "com.edgecore.launcher.tv9x9.message9x9.SET";

	/**
	 * Switch message sent from 9x9tv, to tell system switch to 9x9 home
	 * <p>Input: extra data.
	 * <p>Output: nothing.
	 */
	public static String MESSAGE_SWITCH = "com.edgecore.launcher.tv9x9.message9x9.SWITCH";

	/**
	 * Extra data
	 */
	public static String EXTRA_DATA_9x9APK = "9x9_apk";
	public static String EXTRA_DATA_SYSTEM = "system";
	public static String EXTRA_DATA_VERSION = "version";
	public static String EXTRA_DATA_SYS_SUSPEND = "sys_suspend";
	public static String EXTRA_DATA_SYS_RESUME = "sys_resume";
	public static String EXTRA_DATA_CLEAR_DONE = "clear_done";
	public static String EXTRA_DATA_SET_DONE = "set_done";

	public static String EXTRA_DATA_APK_READY = "apk_ready";
	public static String EXTRA_DATA_CLEAR_CACHE = "cache_data";
	public static String EXTRA_DATA_SET_NET = "net";
	public static String EXTRA_DATA_SET_SCREEN = "screen";
	public static String EXTRA_DATA_SWITCH_HOME = "home";
}