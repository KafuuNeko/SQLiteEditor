package cc.kafuu.sqlite.editor.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

public class MainPagerAdapter extends FragmentPagerAdapter {
    private List<Fragment> mFragments;

    public MainPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);

        mFragments = fragments;
    }

    @Override
    public Fragment getItem(int i) {
        return mFragments.get(i);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

}
