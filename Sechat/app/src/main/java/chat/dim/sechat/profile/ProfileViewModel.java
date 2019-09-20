package chat.dim.sechat.profile;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModel;

import chat.dim.mkm.ID;

public class ProfileViewModel extends ViewModel {

    @SuppressLint("DefaultLocale")
    public String getNumberString(ID identifier) {
        long number = identifier.getNumber();
        String string = String.format("%010d", number);
        string = string.substring(0, 3) + "-" + string.substring(3, 6) + "-" + string.substring(6);
        return string;
    }
}
