package book.twju.timeline.provider.git;

import static book.twju.timeline.provider.git.GitItem.ofCommit;
import static book.twju.timeline.util.Conditions.checkArgument;
import static book.twju.timeline.util.Conditions.checkState;
import static book.twju.timeline.util.Exceptions.guard;
import static book.twju.timeline.util.Iterables.asList;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toList;
import static org.eclipse.jgit.api.Git.cloneRepository;
import static org.eclipse.jgit.api.Git.open;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import book.twju.timeline.model.ItemProvider;

public class GitItemProvider implements ItemProvider<GitItem> {
  
  static final String URI_MUST_NOT_BE_NULL = "Argument 'uri' must not be null.";
  static final String DESTINATION_MUST_NOT_BE_NULL = "Argument 'destination' must not be null.";
  static final String NAME_MUST_NOT_BE_NULL = "Argument 'name' must not be null.";
  static final String FETCH_COUNT_MUST_NOT_BE_NEGATIVE = "Argument 'fetchCount' must not be negative.";
  static final String LATEST_ITEM_MUST_NOT_BE_NULL = "Argument 'latestItem' must not be null.";
  static final String DESTINATION_MUST_BE_A_DIRECTORY = "Destination <%s> must be a directory.";
  static final String GIT_ITEM_PROVIDER_IS_DISPOSED = "GitItemProvider is disposed.";
  static final String UNKNOWN_GIT_ITEM = "GitItem <%s> is unknown in repository <%s>.";
  static final String REPOSITORY_DIRECTORY_IS_EMPTY = "Repository directory <%s> is empty.";
  static final String URI_IS_NOT_VALID = "URI <%s> is not valid.";
  
  private final Git git;
  
  private boolean disposed;

  public GitItemProvider( String uri, String name ) {
    this( uri, new File( getProperty( "java.io.tmpdir" ) ), name );
  }

  public GitItemProvider( String uri, File destination, String name ) {
    checkArgument( uri != null, URI_MUST_NOT_BE_NULL );
    checkArgument( destination != null, DESTINATION_MUST_NOT_BE_NULL );
    checkArgument( name != null, NAME_MUST_NOT_BE_NULL );
    checkArgument( !destination.exists() || destination.isDirectory(), DESTINATION_MUST_BE_A_DIRECTORY, destination );
    
    git = guarded( () -> openRepository( cloneIfNeeded( uri, destination, name ) ) );
  }

  @Override
  public List<GitItem> fetchItems( GitItem oldestItem, int fetchCount ) {
    checkState( !disposed, GIT_ITEM_PROVIDER_IS_DISPOSED );
    checkArgument( fetchCount >= 0, FETCH_COUNT_MUST_NOT_BE_NEGATIVE );
    
    return asList( readCommits( oldestItem, fetchCount ) )
      .stream()
      .filter( commit -> oldestItem == null || !commit.getId().equals( getId( oldestItem ) ) )
      .map( commit -> ofCommit( commit ) )
      .collect( toList() );
  }

  @Override
  public int getNewCount( GitItem item ) {
    return fetchNew( item ).size();
  }

  @Override
  public List<GitItem> fetchNew( GitItem latestItem ) {
    checkState( !disposed, GIT_ITEM_PROVIDER_IS_DISPOSED );
    checkArgument( latestItem != null, LATEST_ITEM_MUST_NOT_BE_NULL );
    
    guarded( () -> git.pull().call() );
    List<RevCommit> commits = asList( guarded( () -> git.log().setMaxCount( 100 ).call() ) );
    return commits
      .subList( 0, computeNewCount( latestItem, commits ) )
      .stream()
      .map( commit -> ofCommit( commit ) )
      .collect( toList() );
  }

  public void dispose() {
    if( !disposed ) {
      git.close();
      disposed = true;
    }
  }

  private File cloneIfNeeded( String uri, File destination, String name ) {
    File result = new File( destination, name );
    if( !result.exists() )  {
      result.mkdirs();
      guarded( () -> cloneRemote( uri, result ) );
    }
    return result;
  }

  private Git cloneRemote( String uri, File repositoryDir ) throws GitAPIException, TransportException {
    try {
      return cloneRepository().setURI( uri ).setDirectory( repositoryDir ).call(); 
    } catch( InvalidRemoteException ire ) {
      throw new IllegalArgumentException( format( URI_IS_NOT_VALID, uri ) );
    }
  }
  
  private Git openRepository( File repositoryDir ) throws IOException {
    try {
      return open( repositoryDir ); 
    } catch ( RepositoryNotFoundException rnfe ) {
      throw new IllegalArgumentException( format( REPOSITORY_DIRECTORY_IS_EMPTY, repositoryDir ) );
    }
  }

  private Iterable<RevCommit> readCommits( GitItem oldestItem, int fetchCount ) {
    if( oldestItem != null ) {
      return guarded( () -> fetchPredecessors( oldestItem, fetchCount ) );
    }
    return guarded( () -> git.log().setMaxCount( fetchCount ).call() );
  }

  private Iterable<RevCommit> fetchPredecessors( GitItem oldestItem, int fetchCount ) throws Exception {
    try {
      return git.log().add( getId( oldestItem ) ).setMaxCount( fetchCount + 1 ).call();
    } catch( MissingObjectException moe ) {
      File directory = git.getRepository().getDirectory();
      throw new IllegalArgumentException( format( UNKNOWN_GIT_ITEM, oldestItem, directory ), moe );
    }
  }

  private static int computeNewCount( GitItem latestItem, List<RevCommit> commits ) {
    Optional<RevCommit> limiter = findLimiter( latestItem, commits );
    if( limiter.isPresent() ) {
      return commits.indexOf( limiter.get() );
    }
    return commits.size();
  }

  private static Optional<RevCommit> findLimiter( GitItem latestItem, List<RevCommit> commits ) {
    return commits
      .stream()
      .filter( commit -> commit.getId().equals( ObjectId.fromString( latestItem.getId() ) ) )
      .findFirst();
  }

  private static ObjectId getId( GitItem oldestItem ) {
    return ObjectId.fromString( oldestItem.getId() );
  }

  private static <T> T guarded( Callable<T> callable ) {
    return guard( callable ).with( IllegalStateException.class );
  }
}