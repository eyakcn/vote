package io.sophone.sdk.wechat.model;

import java.util.List;

/**
 * @author eyakcn
 * @since 4/29/15 AD
 */
public final class MaterialList extends Status {
    public int total_count;
    public int item_count;
    public List<MaterialItem> item;
}
