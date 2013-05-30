/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-10
 * @version 1.0.0
 */
package com.eandroid.view;

import com.eandroid.view.util.RecyclingBitmapDrawable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;



public class RecyclingImageView extends ImageView{

	public RecyclingImageView(Context context) {
		super(context);
	}
	
	public RecyclingImageView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }
    
    public RecyclingImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
 
    /**
     * @see android.widget.ImageView#onDetachedFromWindow()
     */
    @Override
    protected void onDetachedFromWindow() {
        // This has been detached from Window, so clear the drawable
        setImageDrawable(null);
        super.onDetachedFromWindow();
    }

    /**
     * @see android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)
     */
    @Override
    public void setImageDrawable(Drawable drawable) {
        // Keep hold of previous Drawable
        final Drawable previousDrawable = getDrawable();

        // Call super to set new Drawable
        super.setImageDrawable(drawable);

        // Notify new Drawable that it is being displayed
        notifyDrawable(drawable, true);

        // Notify old Drawable so it is no longer being displayed
        notifyDrawable(previousDrawable, false);
    }
    
    @Override
    public void setBackground(Drawable background) {
        // Keep hold of previous Drawable
        final Drawable previousDrawable = getBackground();

        // Call super to set new Drawable
        super.setBackground(background);

        // Notify new Drawable that it is being displayed
        notifyDrawable(background, true);

        // Notify old Drawable so it is no longer being displayed
        notifyDrawable(previousDrawable, false);
    	
    }
    
    @SuppressWarnings("deprecation")
	@Override
    public void setBackgroundDrawable(Drawable background) {
    	 // Keep hold of previous Drawable
        final Drawable previousDrawable = getBackground();

        // Call super to set new Drawable
        super.setBackgroundDrawable(background);

        // Notify new Drawable that it is being displayed
        notifyDrawable(background, true);

        // Notify old Drawable so it is no longer being displayed
        notifyDrawable(previousDrawable, false);
    }

    /**
     * Notifies the drawable that it's displayed state has changed.
     *
     * @param drawable
     * @param isDisplayed
     */
    private static void notifyDrawable(Drawable drawable, final boolean isDisplayed) {
        if (drawable instanceof RecyclingBitmapDrawable) {
            // The drawable is a CountingBitmapDrawable, so notify it
            ((RecyclingBitmapDrawable) drawable).setIsDisplayed(isDisplayed);
        } else if (drawable instanceof LayerDrawable) {
            // The drawable is a LayerDrawable, so recurse on each layer
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            for (int i = 0, z = layerDrawable.getNumberOfLayers(); i < z; i++) {
                notifyDrawable(layerDrawable.getDrawable(i), isDisplayed);
            }
        }
    }
}
