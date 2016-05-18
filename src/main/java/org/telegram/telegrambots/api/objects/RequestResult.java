/**
 * 
 */
package org.telegram.telegrambots.api.objects;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.Constants;

/**
 * Result of telegram polling. If 
 * @author "Maksim Vakhnik"
 *
 */
public class RequestResult {

    private final String origin;
    private final JSONObject jsonObject;
    private final Collection<Update> updates;
    public RequestResult(final String origin) {
        this.origin = origin;
        this.jsonObject = new JSONObject(origin);
        this.updates = updates(origin);
    }


    public Collection<Update> updates() {
        return this.updates;
    }

    
    /**
     * 
     * @param origin
     * @return updates in order of updateId
     */
    private  Collection<Update> updates(final String origin) {
        final Collection<Update> result = new TreeSet<>(new Comparator<Update>() {
            @Override
            public int compare(final Update o1, final Update o2) {
                return o1.getUpdateId().compareTo(o2.getUpdateId());
            }
            
        });
        if (!isOk()) {
            return Collections.emptyList();
        }
        final JSONArray jsonArray =
            this.jsonObject.getJSONArray(Constants.RESPONSEFIELDRESULT);
        for (int i = 0; i < jsonArray.length(); i++) {
            result.add(new Update(jsonArray.getJSONObject(i)));
        }
        return result;
    }
    
    public boolean isOk() {
        return this.jsonObject.getBoolean(Constants.RESPONSEFIELDOK);
    }

    @Override
    public String toString() {
        return this.origin;
    }


    public Optional<Integer> offset() {
        if (!updates().isEmpty()) {
            return Optional.of(
                updates().toArray(
                    new Update[]{}
                    )[updates().size() - 1].getUpdateId());  
        }
        return Optional.empty();
        
    }
    
}
