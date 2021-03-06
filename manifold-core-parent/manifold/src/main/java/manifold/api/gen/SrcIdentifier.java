package manifold.api.gen;

/**
 */
public class SrcIdentifier extends SrcExpression<SrcIdentifier>
{
  public SrcIdentifier( String name )
  {
    name( name );
  }

  @Override
  public SrcIdentifier copy()
  {
    return new SrcIdentifier( getSimpleName() );
  }

  public StringBuilder render( StringBuilder sb, int indent )
  {
    return render( sb, indent, false );
  }

  public StringBuilder render( StringBuilder sb, int indent, boolean sameLine )
  {
    sb.append( getSimpleName() );
    return sb;
  }
}
