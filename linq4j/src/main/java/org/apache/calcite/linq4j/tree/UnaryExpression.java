/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.linq4j.tree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Type;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents an expression that has a unary operator.
 */
public class UnaryExpression extends Expression {
  public final Expression expression;

  UnaryExpression(ExpressionType nodeType, Type type, Expression expression) {
    super(nodeType, type);
    this.expression = requireNonNull(expression, "expression");
  }

  @Override public Expression accept(Shuttle shuttle) {
    shuttle = shuttle.preVisit(this);
    Expression expression = this.expression.accept(shuttle);
    return shuttle.visit(this, expression);
  }

  @Override public <R> R accept(Visitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override void accept(ExpressionWriter writer, int lprec, int rprec) {
    switch (nodeType) {
    case Convert:
      if (!writer.requireParentheses(this, lprec, rprec)) {
        writer.append("(").append(type).append(") ");
        expression.accept(writer, nodeType.rprec, rprec);
      }
      return;
    case ConvertChecked:
      // This is ugly, but Java does not seem to have any facilities
      // to perform checked cast between scalar types!
      // So we use the existing linq4j Primitive.numberValue method
      // which does overflow checking.
      if (!writer.requireParentheses(this, lprec, rprec)) {
        // Generate Java code that looks like e.g.,
        // ((Number)org.apache.calcite.linq4j.tree.Primitive.of(int.class)
        //     .numberValueRoundDown(literal_value)).intValue();
        writer.append("((Number)")
            .append("org.apache.calcite.linq4j.tree.Primitive.of(")
            .append(type)
            .append(".class)")
            .append(".numberValueRoundDown(");
        expression.accept(writer, nodeType.rprec, rprec);
        writer.append(")).").append(type).append("Value()");
      }
      return;
    default:
      break;
    }
    if (nodeType.postfix) {
      expression.accept(writer, lprec, nodeType.rprec);
      writer.append(nodeType.op);
    } else {
      writer.append("(");
      writer.append(nodeType.op);
      expression.accept(writer, nodeType.lprec, rprec);
      writer.append(")");
    }
  }

  @Override public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    UnaryExpression that = (UnaryExpression) o;
    return expression.equals(that.expression);
  }

  @Override public int hashCode() {
    return Objects.hash(nodeType, type, expression);
  }
}
