package sky.client.protect;


import sky.client.manager.Manager;

public class NativeHelper {
    public static void setProfile() {
        Manager.USER_PROFILE = new UserProfile(
                "amf1_",
                "DEV",
                "09.11.2025"
        );
    }
}
