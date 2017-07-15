package org.alexgdev.bizwatch.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.NullHandling;
import org.springframework.data.domain.Sort.Order;

import io.vertx.core.json.JsonObject;

public class RestUtil {
	
	final static String ID_STR = "id";
    final static String SORT = "sort";
    final static String SIZE = "size";
    final static String PAGE = "page";

    final static int DEFAULT_SIZE = Integer.MAX_VALUE;
    final static int DEFAULT_PAGE = 0;
	
    /**
     * Retrieves the sort specification of the query. The sort format in the 
     * request is expected as follows in the query parameter:
     *  <pre>
     *  sort=property[.direction[.nullHandling]][,property[.direction[.nullHandling]]]
     * </pre>
     * 
     * <p>The valid values of directions are: asc, desc. 
     * <p> The valid values of nullHanding are: native, nulls_first, nulls_last.
     * 
     * <p> Examples of query parameters:
     * <ul>
     *   <li>sort=priority,name,created
     *   <li>sort=priority.desc,name,created.asc
     *   <li>sort=priority.desc.nulls_last,name,created.asc
     * </ul>
     * 
     * <p> Override this method to define your customized sort format.
     * 
     * @param request HTTP request
     * @return  The <code>Sort</code> specification
     */
	public static Sort getSort(JsonObject params) {
        String sortStr = params.getString(SORT);
        if (sortStr == null || sortStr.isEmpty()) {
           return null;                         
        }     
        
        String[] properties = sortStr.split(",");
        List<Order> orders = new ArrayList<Order>(properties.length);
        for (String prop : properties) {                                
            String[] details = prop.split("\\.");                
            String propName = details[0];                
            Direction direction = (details.length > 1) ? 
                Direction.fromStringOrNull(details[1]) : Sort.DEFAULT_DIRECTION;              
            NullHandling nullHandling = (details.length > 2) ? 
                NullHandling.valueOf(details[2].toUpperCase(Locale.US)) : null;
                                
            Order order = new Order(direction, propName, nullHandling);
            orders.add(order);
        }      
        return new Sort(orders);
    }
    
    /**
     * Retrieves the paging specification from the request. The paging hint is
     * expected as optional query parameters as <pre>
     *  {@literal page=pageNumber&size=pageSize} </pre>
     * 
     * <p> The page number starts at 0 and is defaulted to 0. The page size is defaulted
     * to 25. Paging is enabled when either page or size hint is present in the
     * request. 
     * 
     * <p> Override this method to define your own paging format.
     * 
     * @param request HTTP request
     * @param sort the <code>Sort</code> specification
     * @return  The <code>Pageable</code> specification
     */
    public static Pageable getPageable(JsonObject params, Sort sort) {
        Pageable result = null;
        String pageStr = params.getString(PAGE);
        String sizeStr = params.getString(SIZE);      
        if (pageStr != null || sizeStr != null)  {
            int page = (pageStr != null) ? Integer.valueOf(pageStr) : DEFAULT_PAGE;
            int size = (sizeStr != null) ? Integer.valueOf(sizeStr) : DEFAULT_SIZE;            
            result = new PageRequest(page, size, sort);
        }        
        return result;
    }

}
