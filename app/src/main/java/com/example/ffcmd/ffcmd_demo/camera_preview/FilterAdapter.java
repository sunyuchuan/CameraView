package com.example.ffcmd.ffcmd_demo.camera_preview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.ffcmd.ffcmd_demo.R;
import com.example.ffcmd.ffcmd_demo.view.utils.XMFilterType;

/**
 * Created by why8222 on 2016/3/17.
 */
public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterHolder>{

    private XMFilterType[] filters;
    private Context context;
    private ButtonListener mButtonListener = null;
    private int lastPosition = -1;

    public FilterAdapter(Context context, XMFilterType[] filters) {
        this.filters = filters;
        this.context = context;
    }

    @Override
    public FilterHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.filter_item_layout,
                parent, false);
        FilterHolder viewHolder = new FilterHolder(view);
        viewHolder.filterName = (TextView) view
                .findViewById(R.id.filter_thumb_name);
        viewHolder.filterRoot = (FrameLayout)view
                .findViewById(R.id.filter_root);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final FilterHolder holder, final int position) {
        holder.filterName.setText(FilterTypeHelper.FilterType2Name(filters[position]));
        holder.filterName.setBackgroundColor(context.getResources().getColor(
                FilterTypeHelper.FilterType2Color(XMFilterType.NONE)));

        mButtonListener = new ButtonListener() {
            @Override
            public void onClick(View v) {
                if(lastPosition == position) {
                    notifyItemChanged(position);
                    onFilterChangeListener.onFilterChanged(filters[position], false, false);
                    lastPosition = -1;
                    return;
                }
                if(lastPosition >= 0) {
                    notifyItemChanged(lastPosition);
                    onFilterChangeListener.onFilterChanged(filters[lastPosition], false, true);
                }
                //notifyItemChanged(position);
                holder.filterName.setBackgroundColor(context.getResources().getColor(
                        FilterTypeHelper.FilterType2Color(filters[position])));
                onFilterChangeListener.onFilterChanged(filters[position], true, true);
                lastPosition = position;
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                /*if (event.getAction() == MotionEvent.ACTION_UP) {
                    notifyItemChanged(position);
                    //holder.filterName.setBackgroundColor(context.getResources().getColor(
                    //FilterTypeHelper.FilterType2Color(XMFilterType.NONE)));
                    onFilterChangeListener.onFilterChanged(filters[position], false);
                }

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //notifyItemChanged(lastSelected);
                    //notifyItemChanged(position);
                    holder.filterName.setBackgroundColor(context.getResources().getColor(
                            FilterTypeHelper.FilterType2Color(filters[position])));
                    onFilterChangeListener.onFilterChanged(filters[position], true);
                }*/
                return false;
            }
        };

        holder.filterRoot.setOnTouchListener(mButtonListener);
        holder.filterRoot.setOnClickListener(mButtonListener);
    }

    @Override
    public int getItemCount() {
        return filters == null ? 0 : filters.length;
    }

    class FilterHolder extends RecyclerView.ViewHolder {
        TextView filterName;
        FrameLayout filterRoot;

        public FilterHolder(View itemView) {
            super(itemView);
        }
    }

    public interface onFilterChangeListener{
        void onFilterChanged(XMFilterType filterType, boolean show, boolean switch_filter);
    }

    private onFilterChangeListener onFilterChangeListener;

    public void setOnFilterChangeListener(onFilterChangeListener onFilterChangeListener){
        this.onFilterChangeListener = onFilterChangeListener;
    }

    abstract class ButtonListener implements View.OnClickListener, View.OnTouchListener {
    }

}
