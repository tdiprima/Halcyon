package com.ebremer.halcyon.datum;

import com.ebremer.halcyon.datum.DataCore.Level;
import static com.ebremer.halcyon.datum.DataCore.Level.OPEN;
import com.ebremer.halcyon.fuseki.shiro.JwtToken;
import com.ebremer.halcyon.gui.HalcyonSession;
import com.ebremer.halcyon.pools.AccessCache;
import com.ebremer.halcyon.pools.AccessCachePool;
import com.ebremer.ns.HAL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.permissions.SecurityEvaluator;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.shared.AuthenticationRequiredException;
import org.apache.jena.vocabulary.SchemaDO;
import org.apache.jena.vocabulary.WAC;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * 
 * @author erich
 */
public final class WACSecurityEvaluator implements SecurityEvaluator {
    private final Level level;
    
    public WACSecurityEvaluator(Level level) {
        this.level = level;
        //System.out.println("==================================== WACSecurityEvaluator ======================================================");
    }

    @Override
    public boolean evaluate(Object principal, Action action, Node graphIRI) {
        //System.out.println("evaluate(Object principal, Action action, Node graphIRI) --> "+action+"   "+graphIRI);
        if (level == OPEN) {
            if (graphIRI.matches(HAL.CollectionsAndResources.asNode())) {
                return true;
            }
        }
        HalcyonPrincipal hp = (HalcyonPrincipal) principal;
        AccessCache ac;
        try {
            ac = AccessCachePool.getPool().borrowObject(hp.getURNUUID());
            if (ac.getCache().containsKey(graphIRI)) {
                if (ac.getCache().get(graphIRI).containsKey(action)) {
                    AccessCachePool.getPool().returnObject(hp.getURNUUID(), ac);
                    return true;
                }
            }
            HashMap<Action,Boolean> set = new HashMap<>();
            ac.getCache().put(graphIRI, set);
            ParameterizedSparqlString pss = new ParameterizedSparqlString("""
                ASK {?rule acl:accessTo/so:hasPart* ?target;
                           acl:mode ?mode;
                           acl:agent ?group
                }
            """);
            pss.setNsPrefix("acl", WAC.NS);
            pss.setNsPrefix("so", SchemaDO.NS);
            pss.setNsPrefix("hal", HAL.NS);
            pss.setIri("target", graphIRI.toString());
            pss.setIri("mode", WACUtil.WAC(action));
            pss.setIri("group", HAL.Anonymous.toString());
            if (QueryExecutionFactory.create(pss.toString(), ac.getSECM()).execAsk()) {
                set.put(action, true);
                AccessCachePool.getPool().returnObject(hp.getURNUUID(), ac);
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
            pss.setIri("target", graphIRI.toString());
            pss.setIri("mode", WACUtil.WAC(action));
            pss.setIri("member", hp.getURNUUID());
            boolean ha = QueryExecutionFactory.create(pss.toString(), ac.getSECM()).execAsk();
            set.put(action, ha);
            AccessCachePool.getPool().returnObject(hp.getURNUUID(), ac);
            return ha;
        } catch (Exception ex) {
            Logger.getLogger(WACSecurityEvaluator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return false;
    }
    
    private boolean evaluate( Object principal, Triple triple ) {
        return evaluate( principal, triple.getSubject()) && evaluate( principal, triple.getObject()) && evaluate( principal, triple.getPredicate());
    }
    
    private boolean evaluate( Object principal, Node node ) {
        return node.equals( Node.ANY );
    }

    @Override
    public boolean evaluate(Object principal, Action action, Node graphIRI, Triple triple) {
        return !evaluate( principal, triple );
    }
    
    @Override
    public boolean evaluate(Object principal, Set<Action> actions, Node graphIRI) throws AuthenticationRequiredException {
        return SecurityEvaluator.super.evaluate(principal, actions, graphIRI);
    }

    @Override
    public boolean evaluate(Object principal, Set<Action> actions, Node graphIRI, Triple triple) throws AuthenticationRequiredException {
        return SecurityEvaluator.super.evaluate(principal, actions, graphIRI, triple);
    }

    @Override
    public boolean evaluateAny(Object principal, Set<Action> actions, Node graphIRI) throws AuthenticationRequiredException {
        return SecurityEvaluator.super.evaluateAny(principal, actions, graphIRI);
    }

    @Override
    public boolean evaluateAny(Object principal, Set<Action> actions, Node graphIRI, Triple triple) throws AuthenticationRequiredException {
        return SecurityEvaluator.super.evaluateAny(principal, actions, graphIRI, triple);
    }

    @Override
    public boolean evaluateUpdate(Object principal, Node graphIRI, Triple from, Triple to) throws AuthenticationRequiredException {
        return SecurityEvaluator.super.evaluateUpdate(principal, graphIRI, from, to);
    }

    @Override
    public Principal getPrincipal() {
        try {
            Subject subject = SecurityUtils.getSubject();
            JwtToken jwttoken = (JwtToken) subject.getPrincipal();
            //Claims dc = (Claims) SecurityUtils.getSubject().getPrincipal();
            return new HalcyonPrincipal(jwttoken,false);
        } catch (org.apache.shiro.UnavailableSecurityManagerException ex) {
            // assume and try for a Keycloak Servlet Filter Auth
        }
        HalcyonPrincipal p = HalcyonSession.get().getHalcyonPrincipal();
        //System.out.println("PRINCIPAL --> "+p.getURNUUID());
        return p;
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
