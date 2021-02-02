package com.beiying.lopnor.base.utils;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

/**
 * Created by tanjie on 15/9/21.
 */
public class ViewUtils {

    /**
     * 获取一定比例的屏幕高度
     */
    public static int getScreenRatioHeight(Context context, double percent) {
        int screenHeight = SystemInfoUtil.getScreenHeight(context);
        return (int) (screenHeight * percent);
    }

    public static int dipToPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int pxToDip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static float convertSpToPixels(Context context, float sp) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
        return px;
    }

    public static void measure(View view, int width, int height) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    }

    public static void layout(View view, int offsetX, int offsetY) {
        view.layout(offsetX, offsetY, offsetX + view.getMeasuredWidth(), offsetY + view.getMeasuredHeight());
    }

    public static Bitmap convert2Bitmap(View view) {
        if(view == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public static Bitmap drawable2Bitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                bitmap =  bitmapDrawable.getBitmap().copy(Bitmap.Config.RGB_565, true);
            }
        } else {
            if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }


        return bitmap;
    }

    public static int getTotalHeightofListView(ListView listView) {
        ListAdapter mAdapter = listView.getAdapter();
        if (mAdapter == null) {
            return 0;
        }
        int totalHeight = 0;
        for (int i = 0; i < mAdapter.getCount();
             i++) {
            View mView = mAdapter.getView(i, null, listView);
            mView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            //mView.measure(0, 0);
            totalHeight += mView.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (mAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();

        return totalHeight;
    }


    /**
     * showDialog loading equals showLoading function in studio
     * studio 也应该调用这个 统一progress 格式
     */
    public static ProgressDialog showProgressDialog(Context context, String title, String msg) {
        return showProgressDialog(context, title, msg, true);
    }

    public static ProgressDialog showProgressDialog(Context context, String title, String msg, boolean cancelable) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(msg);
        dialog.setCancelable(cancelable);
        if (context instanceof Activity) {
            if (!((Activity) context).isFinishing() && !((Activity) context).isDestroyed()) {
                dialog.show();
            }
        }
        return dialog;
    }

    public static ProgressDialog updateDialog(ProgressDialog dialog, String title, String msg) {
        dialog.setMessage(msg);
        dialog.setTitle(title);
        return dialog;
    }

    /**
     * hide loading  equals  hideloading function in studio
     * @param dialog
     */
    public static void dismissDialog(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    public static AlertDialog showAlertDialog(
            Context context, String title, CharSequence message,
            @StringRes int positiveTextId, DialogInterface.OnClickListener positiveListener,
            @StringRes int negativeTextId, DialogInterface.OnClickListener negativeListener) {
        return showAlertDialog(context, title, message, positiveTextId, positiveListener,
                negativeTextId, negativeListener, true);
    }

    public static AlertDialog showAlertDialog(
            Context context, String title, CharSequence message,
            @StringRes int positiveTextId, DialogInterface.OnClickListener positiveListener,
            @StringRes int negativeTextId, DialogInterface.OnClickListener negativeListener,
            boolean cancelable) {
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle(title).setMessage(message)
                .setPositiveButton(positiveTextId, positiveListener)
                .setNegativeButton(negativeTextId, negativeListener)
                .setCancelable(cancelable)
                .create();
        dialog.show();
        return dialog;
    }

    public static MenuItem addMenuItem(Menu menu, @IdRes int itemId,
                                       @StringRes int titleRes, @DrawableRes int icon, int showAsActionFlags) {
        return menu.add(Menu.NONE, itemId, Menu.NONE, titleRes)
                .setIcon(icon)
                .setShowAsActionFlags(showAsActionFlags);
    }

    public static MenuItem addMenuItem(Menu menu, @IdRes int itemId,
                                       @StringRes int titleRes, @DrawableRes int icon) {
        return menu.add(Menu.NONE, itemId, Menu.NONE, titleRes)
                .setIcon(icon)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    public static MenuItem addMenuItem(Menu menu, @IdRes int itemId, @StringRes int titleRes) {
        return menu.add(Menu.NONE, itemId, Menu.NONE, titleRes)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    public static MenuItem addMenuItem(Menu menu, @IdRes int itemId, CharSequence title) {
        return menu.add(Menu.NONE, itemId, Menu.NONE, title)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    public static MenuItem addMenuItem(Menu menu, @IdRes int itemId, CharSequence title, String color) {
        title = Html.fromHtml(String.format("<font color='%1$s'>%2$s</font>", color, title));
        return menu.add(Menu.NONE, itemId, Menu.NONE, title)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    public static MenuItem addMenuItem(Menu menu, @IdRes int itemId,
                                       String title, @DrawableRes int icon) {
        return menu.add(Menu.NONE, itemId, Menu.NONE, title)
                .setIcon(icon)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    public static void setOnPreDrawListener(View view, ViewTreeObserver.OnPreDrawListener listener) {
        if (view != null && listener != null) {
            view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    view.getViewTreeObserver().removeOnPreDrawListener(this);
                    return listener.onPreDraw();
                }
            });
        }
    }

    /**
     * 提示框，带自定义contentView
     * @param context
     * @param title
     * @param contentView
     */
    public static AlertDialog showMessageWithCustomViewDialog(Context context, String title, View contentView,
                                                              @StringRes int positiveTextId, DialogInterface.OnClickListener positiveListener,
                                                              @StringRes int negativeTextId, DialogInterface.OnClickListener negativeListener) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(contentView)
                .setPositiveButton(positiveTextId, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (positiveListener != null) {
                            positiveListener.onClick(dialog, which);
                        }
                    }
                })
                .setNegativeButton(negativeTextId, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (negativeListener != null) {
                            negativeListener.onClick(dialog, which);
                        }
                    }
                })
                .create();
        return alertDialog;
    }

}
