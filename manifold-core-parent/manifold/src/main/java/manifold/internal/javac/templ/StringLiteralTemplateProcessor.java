package manifold.internal.javac.templ;

import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import manifold.api.templ.StringLiteralTemplateParser;
import manifold.api.templ.DisableStringLiteralTemplates;
import manifold.api.type.ICompilerComponent;
import manifold.internal.javac.JavaParser;
import manifold.util.Stack;

public class StringLiteralTemplateProcessor extends TreeTranslator implements ICompilerComponent, TaskListener
{
  private BasicJavacTask _javacTask;
  private TreeMaker _maker;
  private Names _names;
  private Stack<Boolean> _disabled;

  @Override
  public void init( BasicJavacTask javacTask )
  {
    _javacTask = javacTask;
    _maker = TreeMaker.instance( _javacTask.getContext() );
    _names = Names.instance( _javacTask.getContext() );
    _disabled = new Stack<>();
    _disabled.push( false );

    javacTask.addTaskListener( this );
  }

  @Override
  public void started( TaskEvent taskEvent )
  {
    // nothing to do
  }

  @Override
  public void finished( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.PARSE )
    {
      return;
    }

    for( Tree tree : e.getCompilationUnit().getTypeDecls() )
    {
      if( !(tree instanceof JCTree.JCClassDecl) )
      {
        continue;
      }
      
      JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl)tree;
      classDecl.accept( this );
    }
  }

  @Override
  public void visitClassDef( JCTree.JCClassDecl classDef )
  {
    process( classDef.getModifiers(), () -> super.visitClassDef( classDef ) );
  }

  @Override
  public void visitMethodDef( JCTree.JCMethodDecl methodDecl )
  {
    process( methodDecl.getModifiers(), () -> super.visitMethodDef( methodDecl ) );
  }

  @Override
  public void visitVarDef( JCTree.JCVariableDecl varDecl )
  {
    process( varDecl.getModifiers(), () -> super.visitVarDef( varDecl ) );
  }

  private void process( JCTree.JCModifiers modifiers, Runnable processor )
  {
    Boolean disable = getDisableAnnotationValue( modifiers );
    if( disable != null )
    {
      pushDisabled( disable );
    }

    try
    {
      // processor
      processor.run();
    }
    finally
    {
      if( disable != null )
      {
        popDisabled( disable );
      }
    }
  }

  private Boolean getDisableAnnotationValue( JCTree.JCModifiers modifiers )
  {
    Boolean disable = null;
    Boolean disableGlobally = Boolean.valueOf(System.getProperty("manifold.string.template.disable"));
    if( disableGlobally) {
      return true;
    } else {
      for( JCTree.JCAnnotation anno: modifiers.getAnnotations() )
      {
        if( anno.annotationType.toString().contains( DisableStringLiteralTemplates.class.getSimpleName() ) )
        {
          try
          {
            com.sun.tools.javac.util.List<JCTree.JCExpression> args = anno.getArguments();
            if( args.isEmpty() )
            {
              disable = true;
            }
            else
            {
              JCTree.JCExpression argExpr = args.get( 0 );
              Object value;
              if( argExpr instanceof JCTree.JCLiteral &&
                  (value = ((JCTree.JCLiteral)argExpr).getValue()) instanceof Boolean )
              {
                disable = (boolean)value;
              }
              else
              {
                Log.instance( _javacTask.getContext() ).error( argExpr.pos(),
                  "proc.messager", "Only boolean literal values 'true' and 'false' allowed here" );
                disable = true;
              }
            }
          }
          catch( Exception e )
          {
            disable = true;
          }
        }
      }

    }
    return disable;
  }

  private boolean isDisabled()
  {
    return _disabled.peek();
  }
  private void pushDisabled( boolean disabled )
  {
    _disabled.push( disabled );
  }
  private void popDisabled( boolean disabled )
  {
    if( disabled != _disabled.pop() )
    {
      throw new IllegalStateException();
    }
  }

  @Override
  public void visitLiteral( JCTree.JCLiteral jcLiteral )
  {
    super.visitLiteral( jcLiteral );

    Object value = jcLiteral.getValue();
    if( !(value instanceof String) )
    {
      return;
    }

    if( isDisabled() )
    {
      return;
    }

    String stringValue = (String)value;
    List<JCTree.JCExpression> exprs = parse( stringValue, jcLiteral.getPreferredPosition() );
    JCTree.JCBinary concat = null;
    while( !exprs.isEmpty() )
    {
      if( concat == null )
      {
        concat = _maker.Binary( JCTree.Tag.PLUS, exprs.remove( 0 ), exprs.remove( 0 ) );
      }
      else
      {
        concat = _maker.Binary( JCTree.Tag.PLUS, concat, exprs.remove( 0 ) );
      }
    }

    result = concat == null ? result : concat;
  }

  public List<JCTree.JCExpression> parse( String stringValue, int literalOffset )
  {
    List<StringLiteralTemplateParser.Expr> comps = StringLiteralTemplateParser.parse( stringValue );
    if( comps.isEmpty() )
    {
      return Collections.emptyList();
    }

    List<JCTree.JCExpression> exprs = new ArrayList<>();
    StringLiteralTemplateParser.Expr prev = null;
    for( StringLiteralTemplateParser.Expr comp : comps )
    {
      JCTree.JCExpression expr;
      if( comp.isVerbatim() )
      {
        expr = _maker.Literal( comp.getExpr() );
      }
      else
      {
        if( prev != null && !prev.isVerbatim() )
        {
          // enforce concatenation
          exprs.add( _maker.Literal( "" ) );
        }

        int exprPos = literalOffset + 1 + comp.getOffset();

        if( comp.isIdentifier() )
        {
          JCTree.JCIdent ident = _maker.Ident( _names.fromString( comp.getExpr() ) );
          ident.pos = exprPos;
          expr = ident;
        }
        else
        {
          DiagnosticCollector<JavaFileObject> errorHandler = new DiagnosticCollector<>();
          expr = JavaParser.instance().parseExpr( comp.getExpr(), errorHandler );
          if( transferParseErrors( literalOffset, comp, expr, errorHandler ) )
          {
            return Collections.emptyList();
          }
          replaceNames( expr, exprPos );
        }
      }
      prev = comp;
      exprs.add( expr );
    }

    if( exprs.size() == 1 )
    {
      // insert an empty string so concat will make the expr a string
      exprs.add( 0, _maker.Literal( "" ) );
    }

    return exprs;
  }

  private boolean transferParseErrors( int literalOffset, StringLiteralTemplateParser.Expr comp, JCTree.JCExpression expr, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    if( expr == null || errorHandler.getDiagnostics().stream().anyMatch( e -> e.getKind() == Diagnostic.Kind.ERROR ) )
    {
      for( Diagnostic<? extends JavaFileObject> diag : errorHandler.getDiagnostics() )
      {
        if( diag.getKind() == Diagnostic.Kind.ERROR )
        {
          JCDiagnostic jcDiag = ((ClientCodeWrapper.DiagnosticSourceUnwrapper)diag).d;
          String code = debaseMsgCode( diag );
          Log.instance( _javacTask.getContext() ).warning( new JCDiagnostic.SimpleDiagnosticPosition( literalOffset + 1 + comp.getOffset() ), code, jcDiag.getArgs() );
        }
      }
      return true;
    }
    return false;
  }

  private String debaseMsgCode( Diagnostic<? extends JavaFileObject> diag )
  {
    // Log#error() will prepend "compiler.err", so we must remove it to avoid double-basing the message
    String code = diag.getCode();
    if( code != null && code.startsWith( "compiler.err" ) )
    {
      code = code.substring( "compiler.err".length() + 1 );
    }
    return code;
  }

  private void replaceNames( JCTree.JCExpression expr, int offset )
  {
    expr.accept( new NameReplacer( _javacTask, offset ) );
  }
}
