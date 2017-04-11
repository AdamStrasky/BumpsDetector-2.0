package navigationapp.server;

/**
 * Created by Adam on 11.4.2017.
 */

public @interface Connection {
    public static String create_bump = "http://vytlky.fiit.stuba.sk//create_bump.php";
    public static String get_image = "http://vytlky.fiit.stuba.sk//get_image.php";
    public static String get_image_id = "http://vytlky.fiit.stuba.sk//get_image_id.php";
    public static String sync_bump = "http://vytlky.fiit.stuba.sk//sync_bump.php";
    public static String update_image = "http://vytlky.fiit.stuba.sk//update_image.php";
}
