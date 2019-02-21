package com.cbbase.core.connector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import com.cbbase.core.tools.ObjectUtil;

/**
 * 
 * @author changbo
 *
 */
public class EsGroupHelper {
	
	private SearchRequestBuilder searchRequestBuilder;
	private BoolQueryBuilder boolQueryBuilder;
	private List<TermsAggregationBuilder> termsList = new ArrayList<>();
	private List<AggregationBuilder> aggList = new ArrayList<>();
	private int size = 10;
	private SearchResponse response;
	
	private EsGroupHelper(SearchRequestBuilder searchRequestBuilder) {
		this.searchRequestBuilder = searchRequestBuilder;
	}
	
	public static EsGroupHelper getEsGroupHelper(SearchRequestBuilder searchRequestBuilder) {
		return new EsGroupHelper(searchRequestBuilder);
	}
	
	private BoolQueryBuilder getQueryBuilder() {
		if(boolQueryBuilder == null) {
			boolQueryBuilder = QueryBuilders.boolQuery();
		}
		return boolQueryBuilder;
	}
	
	public EsGroupHelper setQueryBuilder(BoolQueryBuilder boolQueryBuilder) {
		this.boolQueryBuilder = boolQueryBuilder;
		return this;
	}
	
	
	public EsGroupHelper andEqual(String field, Object value) {
		getQueryBuilder().must(QueryBuilders.termQuery(field, value));
		return this;
	}
	
	public EsGroupHelper andLike(String field, Object value) {
		getQueryBuilder().must(QueryBuilders.fuzzyQuery(field, value));
		return this;
	}
	
	public EsGroupHelper andNotEqual(String field, Object value) {
		getQueryBuilder().mustNot(QueryBuilders.termQuery(field, value));
		return this;
	}
	
	public EsGroupHelper andNotLike(String field, Object value) {
		getQueryBuilder().mustNot(QueryBuilders.fuzzyQuery(field, value));
		return this;
	}
	
	public EsGroupHelper orEqual(String field, Object value) {
		getQueryBuilder().should(QueryBuilders.termQuery(field, value));
		return this;
	}
	
	public EsGroupHelper orLike(String field, Object value) {
		getQueryBuilder().should(QueryBuilders.commonTermsQuery(field, value));
		return this;
	}
	
	public EsGroupHelper range(String field, Object from, Object to) {
		getQueryBuilder().must(QueryBuilders.rangeQuery(field).gte(from).lte(to));
		return this;
	}
	
	public EsGroupHelper groupBy(String alias, String field) {
		termsList.add(AggregationBuilders.terms(alias).field(field));
		return this;
	}
	
	public EsGroupHelper size(int size) {
		this.size = size;
		return this;
	}
	
	public EsGroupHelper sum(String alias, String field) {
		aggList.add(AggregationBuilders.sum(alias).field(field));
		return this;
	}
	
	public EsGroupHelper avg(String alias, String field) {
		aggList.add(AggregationBuilders.avg(alias).field(field));
		return this;
	}
	
	public EsGroupHelper count(String alias, String field) {
		aggList.add(AggregationBuilders.count(alias).field(field));
		return this;
	}
	
	public EsGroupHelper max(String alias, String field) {
		aggList.add(AggregationBuilders.max(alias).field(field));
		return this;
	}
	
	public EsGroupHelper min(String alias, String field) {
		aggList.add(AggregationBuilders.min(alias).field(field));
		return this;
	}
	
	public EsGroupHelper orderBy(boolean asc) {
		if(termsList.size() > 0) {
			termsList.get(termsList.size()-1).order(BucketOrder.count(asc));
		}
		return this;
	}
	
	public EsGroupHelper orderBy(String path, boolean asc) {
		if(termsList.size() > 0) {
			termsList.get(termsList.size()-1).order(BucketOrder.aggregation(path, asc));
		}
		return this;
	}
	
	public List<Map<String, Object>> execute() {
		if(termsList.size() == 0) {
			return null;
		}
		TermsAggregationBuilder last = null;
		for(TermsAggregationBuilder terms : termsList) {
			if(last == null) {
				last = terms;
			}else {
				last.subAggregation(terms);
				last = terms;
			}
		}
		for(AggregationBuilder agg : aggList) {
			last.subAggregation(agg);
		}
		termsList.get(0).size(size);
		searchRequestBuilder.addAggregation(termsList.get(0));
		searchRequestBuilder.setQuery(boolQueryBuilder);
		response = searchRequestBuilder.execute().actionGet();
		Terms terms = response.getAggregations().get(termsList.get(0).getName());
        return termsToList(terms);
	}
	
	private List<Map<String, Object>> termsToList(Terms terms) {
		List<Map<String, Object>> list = new ArrayList<>();
        for(Terms.Bucket entry : terms.getBuckets()){
        	List<Aggregation> subAggList = entry.getAggregations().asList();
        	Map<String, Object> map = new HashMap<>();
    		map.put(terms.getName(), entry.getKey());
        	for(Aggregation subAgg : subAggList) {
        		if(subAgg instanceof Terms) {
        			Terms sub = (Terms) subAgg;
        			List<Map<String, Object>> subList = termsToList(sub);
        			if(subList.size() == 1) {
        				map.putAll(subList.get(0));
        			}else if(subList.size() > 1){
        				map.put(sub.getName(), subList);
        			}
        		}else {
            		Object val = ObjectUtil.getFieldValue(entry.getAggregations().get(subAgg.getName()), "value");
    	        	map.put(subAgg.getName(), val);
        		}
        	}
        	list.add(map);
        }
    	return list;
	}

}