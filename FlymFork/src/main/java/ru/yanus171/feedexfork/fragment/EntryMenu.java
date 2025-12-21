package ru.yanus171.feedexfork.fragment;

import static ru.yanus171.feedexfork.activity.EntryActivity.GetIsActionBarHidden;
import static ru.yanus171.feedexfork.fragment.EntryFragment.DpToPx;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.NonNull;

import ru.yanus171.feedexfork.R;

public class EntryMenu {
    private final Activity mActivity;
    private int mMenuID = R.menu.entry;

    public EntryMenu(Activity activity) {
        mActivity = activity;
    }
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if ( mMenuID == R.menu.entry ) {
            inflater.inflate(R.menu.entry, menu);
            inflateSubMenu(menu, inflater, R.id.menu_share_group, R.menu.entry_menu_share);
            inflateSubMenu(menu, inflater, R.id.menu_display_options_group, R.menu.entry_menu_display);
            inflateSubMenu(menu, inflater, R.id.menu_reload_group, R.menu.entry_menu_reload);
            menu.findItem(R.id.menu_star).setShowAsAction( GetIsActionBarHidden() ? MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW : MenuItem.SHOW_AS_ACTION_IF_ROOM );
        } else
            inflater.inflate(mMenuID, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            menu.setGroupDividerEnabled( true );
        for ( int i = 0; i < menu.size(); i++ )
            updateMenuWithIcon( menu.getItem( i ) );
    }

    private static void inflateSubMenu(Menu menu, MenuInflater inflater, int subMenuItemID, int subMenuID) {
        SubMenu subMenuShare = menu.findItem(subMenuItemID).getSubMenu();
        inflater.inflate(subMenuID, subMenuShare);
    }

    public static void updateMenuWithIcon(@NonNull final MenuItem item) {
        SpannableStringBuilder builder = new SpannableStringBuilder()
                .append("*") // the * will be replaced with the icon via ImageSpan
                .append("    ") // This extra space acts as padding. Adjust as you wish
                .append(item.getTitle());

        // Retrieve the icon that was declared in XML and assigned during inflation
        if (item.getIcon() != null && item.getIcon().getConstantState() != null) {
            Drawable drawable = item.getIcon().getConstantState().newDrawable();

            // Mutate this drawable so the tint only applies here
            // drawable.mutate().setTint(color);

            // Needs bounds, or else it won't show up (doesn't know how big to be)
            drawable.setBounds(0, 0, DpToPx( 30 ), DpToPx( 30 ) );
            ImageSpan imageSpan = new ImageSpan(drawable);
            builder.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.setTitle(builder);
        }
    }

    public void openShare() {
        openOptionsMenu(R.menu.entry_menu_share);
    }

    public void openReload() {
        openOptionsMenu(R.menu.entry_menu_reload);
    }
    public void openDisplay() {
        openOptionsMenu(R.menu.entry_menu_display);
    }

    public void open() {
        openOptionsMenu(R.menu.entry);
    }

    private void openOptionsMenu( int menuID ) {
        mMenuID = menuID;
        mActivity.invalidateOptionsMenu();
        mActivity.openOptionsMenu();
    }

    static public void setItemChecked( Menu menu, int itemID, boolean checked ) {
        setVisible( menu, itemID );
        MenuItem item = menu.findItem( itemID );
        if (  item != null )
            item.setChecked( checked );
    }
    static public void setVisible(Menu menu, int itemID ) {
        setItemVisible( menu, itemID, true );
    }

    static public void setItemVisible( Menu menu, int itemID, boolean visible ) {
        MenuItem item = menu.findItem( itemID );
        if (  item != null )
            item.setVisible( visible );
    }
}
