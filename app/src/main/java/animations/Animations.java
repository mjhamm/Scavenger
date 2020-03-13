package animations;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class Animations {

    public static void expand(View view) {
        Animation animation = expandAction(view);
        view.startAnimation(animation);
    }

    public static void collapse(View view) {
        Animation animation = collapseAction(view);
        view.startAnimation(animation);
    }

    private static Animation expandAction(final View view) {

        view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int actualheight = view.getMeasuredHeight();

        view.getLayoutParams().height = 0;
        view.setVisibility(View.VISIBLE);

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                if (interpolatedTime == 1) {
                    view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    view.getLayoutParams().height = (int) (actualheight * interpolatedTime);
                    view.requestLayout();
                }
            }
        };


        animation.setDuration((long) (actualheight / view.getContext().getResources().getDisplayMetrics().density));

        view.startAnimation(animation);

        return animation;


    }

    private static Animation collapseAction(final View view) {

        final int height = view.getMeasuredHeight();

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                if (interpolatedTime == 1) {
                    view.setVisibility(View.GONE);
                } else {
                    view.getLayoutParams().height = height - (int) (height * interpolatedTime);
                    view.requestLayout();
                }
            }
        };

        animation.setDuration((long) (height/ view.getContext().getResources().getDisplayMetrics().density));
        view.startAnimation(animation);

        return animation;
    }
}
