package com.ebremer.halcyon.data;

import com.ebremer.halcyon.data.DataCore.Level;
import com.ebremer.halcyon.datum.HalcyonPrincipal;
import static com.ebremer.halcyon.data.DataCore.Level.OPEN;
import com.ebremer.halcyon.fuseki.shiro.JwtToken;
import com.ebremer.halcyon.gui.HalcyonSession;
import com.ebremer.halcyon.pools.AccessCache;
import com.ebremer.halcyon.pools.AccessCachePool;
import com.ebremer.ns.HAL;
import com.ebremer.ns.WAC;
import java.security.Principal;
import java.util.HashMap;
import java.util.Set;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.permissions.SecurityEvaluator;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.vocabulary.SchemaDO;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;

/**
 * 
 * @author erich
 */
public final class WACSecurityEvaluator implements SecurityEvaluator {
    private final Level level;
    
    public WACSecurityEvaluator(Level level) {
        this.level = level;
    }

    @Override
    public boolean evaluate(Object principal, Action action, Node node) {
        if (node.equals(Node.ANY)) {
            return false;
        }
        if (level == OPEN) {
            if (node.matches(HAL.CollectionsAndResources.asNode())) {
                return true;
            }
        }
        HalcyonPrincipal hp = (HalcyonPrincipal) principal;
        AccessCache ac;
        try {
            ac = AccessCachePool.getPool().borrowObject(hp.getUserURI());
        } catch (Exception ex) {
            return false;
        }
        if (ac.getCache().containsKey(node)) {
            if (ac.getCache().get(node).containsKey(action)) {
                boolean ha = ac.getCache().get(node).get(action);
                AccessCachePool.getPool().returnObject(hp.getUserURI(), ac);
                return ha;
            }
        }
        HashMap<Action,Boolean> set = new HashMap<>();
        ac.getCache().put(node, set);
        ParameterizedSparqlString pss = new ParameterizedSparqlString("""
            ASK {?rule acl:accessTo/so:hasPart* ?target;
                        acl:mode ?mode;
                        acl:agent ?group
            }
        """);
        pss.setNsPrefix("acl", WAC.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setIri("target", node.toString());
        pss.setIri("mode", WACUtil.WAC(action));
        pss.setIri("group", HAL.Anonymous.toString());
        if (QueryExecutionFactory.create(pss.toString(), ac.getSECM()).execAsk()) {
            set.put(action, true);
            AccessCachePool.getPool().returnObject(hp.getUserURI(), ac);
            return true;
        }
        pss = new ParameterizedSparqlString("""
            ASK {?rule acl:accessTo/so:hasPart* ?target;
                        acl:mode ?mode;
                        acl:agent ?group .
                ?group so:member ?member
            }
        """);
        pss.setNsPrefix("acl", WAC.NS);
        pss.setNsPrefix("so", SchemaDO.NS);
        pss.setNsPrefix("hal", HAL.NS);
        pss.setIri("target", node.toString());
        pss.setIri("mode", WACUtil.WAC(action));
        pss.setIri("member", hp.getUserURI());
        boolean ha = QueryExecutionFactory.create(pss.toString(), ac.getSECM()).execAsk();
        set.put(action, ha);
        AccessCachePool.getPool().returnObject(hp.getUserURI(), ac);
        return ha;

    }

    @Override
    public boolean evaluate(Object principal, Action action, Node graphIRI, Triple triple) {
        //return evaluate( principal, triple );
        return evaluate( principal, action, triple.getSubject());
        //return evaluate( principal, action, triple.getSubject()) && evaluate( principal, action, triple.getObject()) && evaluate( principal, action, triple.getPredicate());
    }
    
    //private boolean evaluate( Object principal, Triple triple ) {
      //  return evaluate( principal, triple.getSubject()) && evaluate( principal, triple.getObject()) && evaluate( principal, triple.getPredicate());
    //}
    
    @Override
    public boolean evaluate(Object principal, Set<Action> actions, Node graphIRI) {
        return SecurityEvaluator.super.evaluate(principal, actions, graphIRI);
    }

    @Override
    public boolean evaluate(Object principal, Set<Action> actions, Node graphIRI, Triple triple) {
        return SecurityEvaluator.super.evaluate(principal, actions, graphIRI, triple);
    }
    
    /*
    private boolean evaluate( Object principal, Node node ) {
        if (node.equals(Node.ANY)) {
            return false; // all wild cards are false
        }
        return node.equals( Node.ANY );
    }*/


    @Override
    public boolean evaluateAny(Object principal, Set<Action> actions, Node graphIRI) {
        return SecurityEvaluator.super.evaluateAny(principal, actions, graphIRI);
    }

    @Override
    public boolean evaluateAny(Object principal, Set<Action> actions, Node graphIRI, Triple triple) {
        return SecurityEvaluator.super.evaluateAny(principal, actions, graphIRI, triple);
    }

    @Override
    public boolean evaluateUpdate(Object principal, Node graphIRI, Triple from, Triple to) {
        return SecurityEvaluator.super.evaluateUpdate(principal, graphIRI, from, to);
    }

    @Override
    public Principal getPrincipal() {
        try {
            return ((JwtToken) SecurityUtils.getSubject().getPrincipal()).getPrincipal();
        } catch (UnavailableSecurityManagerException ex) {
            // assume and try for a Keycloak Servlet Filter Auth
        }
        //return new HalcyonPrincipal("https://ebremer.com/profile#me");
        return HalcyonSession.get().getHalcyonPrincipal();
    }

    @Override
    public boolean isPrincipalAuthenticated(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isHardReadError() {
        return SecurityEvaluator.super.isHardReadError();
    }
}
