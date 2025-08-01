/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/

package org.pentaho.platform.engine.security.authorization.core.rules;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.pentaho.platform.api.engine.security.authorization.IAuthorizationContext;
import org.pentaho.platform.api.engine.security.authorization.IAuthorizationRequest;
import org.pentaho.platform.api.engine.security.authorization.IAuthorizationRule;
import org.pentaho.platform.api.engine.security.authorization.decisions.IAuthorizationDecision;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractCompositeAuthorizationRule extends AbstractAuthorizationRule
  implements IAuthorizationRule {

  @NonNull
  private final List<IAuthorizationRule> rules;

  protected AbstractCompositeAuthorizationRule( @NonNull List<IAuthorizationRule> rules ) {
    this.rules = List.copyOf( Objects.requireNonNull( rules ) );
  }

  @NonNull
  public List<IAuthorizationRule> getRules() {
    return rules;
  }

  @NonNull
  @Override
  public Optional<IAuthorizationDecision> authorize( @NonNull IAuthorizationRequest request,
                                                     @NonNull IAuthorizationContext context ) {

    AbstractCompositeResultBuilder resultBuilder = createResultBuilder( context );

    for ( IAuthorizationRule rule : getRules() ) {

      Optional<IAuthorizationDecision> ruleResult = context.authorizeRule( request, rule );
      if ( ruleResult.isPresent() ) {
        resultBuilder.withDecision( ruleResult.get() );

        if ( resultBuilder.isImmutable() ) {
          // If the decision is immutable, no need to evaluate other rules.
          break;
        }
      }
    }

    return resultBuilder.build( request );
  }

  @NonNull
  protected abstract AbstractCompositeResultBuilder createResultBuilder( @NonNull IAuthorizationContext context );
}
