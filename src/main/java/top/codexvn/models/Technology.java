package top.codexvn.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Data
public class Technology implements Comparable<Technology> {
    public String key;
    public String name;
    public String description;

    public String area;
    public Float base_factor = 1.0f;
    public String base_weight;
    public String category;
    public String categoryName;
    public Integer cost = 0;

    public boolean is_dangerous;
    public boolean is_rare = false;
    public boolean is_start_tech = false;

    public List<Pair<String,List<String>>> prerequisites = new ArrayList<>();
    public Integer tier;
    public Integer index;
    public List<Pair<String,List<String>>> prerequisites_names = new ArrayList<>();
//    public List<WeightModifier> weight_modifiers = new ArrayList<>();
//    public List<Modifier> potential = new ArrayList<>();
    public SortedSet<Technology> children = new TreeSet<>();

    public boolean is_event = false;

    public Boolean is_gestalt = null;
    public Boolean is_megacorp = null;
    public Boolean is_machine_empire = null;
    public Boolean is_hive_empire = null;

    public Boolean is_drive_assimilator = null;
    public Boolean is_rogue_servitor = null;

    public Technology() {
    }

    public Technology(Technology... children) {
        this.children.addAll(Arrays.asList(children));
    }

    @Override
    public int compareTo(Technology that) {
        final int EQUAL = 0;

        if (this == that) return EQUAL;
        if (this.equals(that)) return EQUAL;

        if(this.key.equals("tech_bio_reactor") || that.key.equals("tech_bio_reactor")) {
            System.out.println(this + " vs " + that);

        }
        int comparison = EQUAL;

        if(that.children.size() == 0 && this.children.size() > 0) comparison = -1;
        if(that.children.size() > 0 && this.children.size() == 0) comparison = 1;
        if (comparison != EQUAL) return comparison;

        comparison = this.area.compareTo(that.area);
        if (comparison != EQUAL) return comparison;

        comparison = this.category.compareTo(that.category);
        if (comparison != EQUAL) return comparison;

        if(this.tier == null) this.tier = 0;
        comparison = Integer.valueOf(this.tier).compareTo(Integer.valueOf(that.tier));
        if (comparison != EQUAL) return comparison;

        comparison = this.cost.compareTo(that.cost);
        if (comparison != EQUAL) return comparison;

        return 1;
    }
}
