package jagoclient.igs.users;

import java.util.List;

/**
 * Created by johndaniels on 7/20/17.
 */
public interface UsersChangedHandler {
    void usersChanged(List<UserInfo> userInfoList);
}
