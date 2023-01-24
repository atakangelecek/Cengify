import java.util.ArrayList;

public class PlaylistTree {
	
	public PlaylistNode primaryRoot;		//root of the primary B+ tree
	public PlaylistNode secondaryRoot;	//root of the secondary B+ tree
	public PlaylistTree(Integer order) {
		PlaylistNode.order = order;
		primaryRoot = new PlaylistNodePrimaryLeaf(null);
		primaryRoot.level = 0;
		secondaryRoot = new PlaylistNodeSecondaryLeaf(null);
		secondaryRoot.level = 0;
	}

	public PlaylistNodePrimaryLeaf node_to_insert(Integer audioId){
		PlaylistNode node = primaryRoot;
		boolean is_greater = false;
		while(node.getType() != PlaylistNodeType.Leaf) {
			is_greater = false;
			for (int i = 0; i < ((PlaylistNodePrimaryIndex) node).audioIdCount(); i++) {
				if (((PlaylistNodePrimaryIndex) node).audioIdAtIndex(i) > audioId) {
					node = ((PlaylistNodePrimaryIndex) node).getChildrenAt(i);
					is_greater = true;
					break;
				}
			}
			if (is_greater == false) {
				node = ((PlaylistNodePrimaryIndex) node).getChildrenAt(((PlaylistNodePrimaryIndex) node).audioIdCount());
			}
		}
			return (PlaylistNodePrimaryLeaf) node;
	}

	public PlaylistNodeSecondaryLeaf node_to_insert_secondary(String genre){
		PlaylistNode node = secondaryRoot;
		boolean is_greater = false;
		while(node.getType() != PlaylistNodeType.Leaf) {
			is_greater = false;

			for (int i = 0; i < ((PlaylistNodeSecondaryIndex) node).genreCount(); i++) {
				if ( (((PlaylistNodeSecondaryIndex) node).genreAtIndex(i).compareTo(genre) > 0) ){
					node = ((PlaylistNodeSecondaryIndex) node).getChildrenAt(i);
					is_greater = true;
					break;
				}
			}
			if (is_greater == false) {
				node = ((PlaylistNodeSecondaryIndex) node).getChildrenAt(((PlaylistNodeSecondaryIndex) node).genreCount());
			}
		}
		return (PlaylistNodeSecondaryLeaf) node;
	}

	public void add_primary(CengSong song){
		// Primary Tree
		// Find the correct leaf to insert song

		PlaylistNodePrimaryLeaf leaf_to_add = node_to_insert(song.audioId());

		ArrayList<CengSong> songs_at_leaf = leaf_to_add.getSongs();
		Integer index = -1;
		for(int i=0; i<songs_at_leaf.size(); i++){
			if(song.audioId() < songs_at_leaf.get(i).audioId()){
				index = i;
				break;
			}
		}
		if(index == -1){
			index = songs_at_leaf.size();
		}

		// add song to the leaf
		songs_at_leaf.add(index, song);
		Integer size = leaf_to_add.songCount();

		// if no overflow
		if(size <= (2*PlaylistNode.order)){
			return ;
		}

		// if overflow at leaf && leaf_to_add == root
		else if(leaf_to_add.getParent() == null){
			//copy up
			PlaylistNodePrimaryIndex new_root = new PlaylistNodePrimaryIndex(null);
			PlaylistNodePrimaryLeaf new_node = new PlaylistNodePrimaryLeaf(new_root);
			Integer j=0;
			for(int i = (size/2); i<size; i++){
				new_node.addSong(j, leaf_to_add.songAtIndex(i));
				j++;
			}
			for(int i = size-1; i>=size/2; i--){
				leaf_to_add.getSongs().remove(i);
			}
			leaf_to_add.setParent(new_root);
			new_root.add_audioId(0, new_node.songAtIndex(0).audioId());
			new_root.getAllChildren().add(0, leaf_to_add);
			new_root.getAllChildren().add(1, new_node);
			primaryRoot = new_root;

		}

		// else if overflow at leaf && leaf_to_add != root
		else if(leaf_to_add.getParent() != null){
			PlaylistNode parent = leaf_to_add.getParent();
			PlaylistNodePrimaryLeaf new_node = new PlaylistNodePrimaryLeaf(parent);
			Integer k=0;
			for(int i = (size/2); i<size; i++){
				new_node.addSong(k, leaf_to_add.songAtIndex(i));
				k++;
			}
			for(int i = size-1; i>=size/2; i--){
				leaf_to_add.getSongs().remove(i);
			}

			// copy up the key to its place
			Integer copy_up_key = new_node.songAtIndex(0).audioId();
			index = -1;
			for(int i=0; i<((PlaylistNodePrimaryIndex)parent).audioIdCount(); i++){
				if(copy_up_key < ((PlaylistNodePrimaryIndex)parent).audioIdAtIndex(i)){
					((PlaylistNodePrimaryIndex)parent).add_audioId(i, copy_up_key);
					((PlaylistNodePrimaryIndex)parent).getAllChildren().add(i+1,new_node);
					index = i;
					break;
				}
			}
			if(index == -1){
				((PlaylistNodePrimaryIndex)parent).add_audioId(copy_up_key);
				((PlaylistNodePrimaryIndex)parent).getAllChildren().add(new_node);
			}

			// check if overflow occurs at parent when we copy up
			while(((PlaylistNodePrimaryIndex)parent).audioIdCount() > (2*PlaylistNode.order)){
				PlaylistNode parent_next;

				// if overflowed parent is root
				if(parent.getParent() == null){
					PlaylistNodePrimaryIndex new_root = new PlaylistNodePrimaryIndex(null);
					PlaylistNodePrimaryIndex new_int_node = new PlaylistNodePrimaryIndex(new_root);

					//now move up instead of copy up
					Integer parent_size = ((PlaylistNodePrimaryIndex) parent).audioIdCount();
					Integer j=0;
					for(int i=(parent_size/2+1); i<parent_size; i++){
						new_int_node.add_audioId(j, ((PlaylistNodePrimaryIndex) parent).audioIdAtIndex(i));
						new_int_node.getAllChildren().add(j, ((PlaylistNodePrimaryIndex) parent).getChildrenAt(i));
						((PlaylistNodePrimaryIndex) parent).getChildrenAt(i).setParent(new_int_node);
						j++;
					}
					Integer last = ((PlaylistNodePrimaryIndex) parent).audioIdCount();
					new_int_node.getAllChildren().add(new_int_node.audioIdCount(), ((PlaylistNodePrimaryIndex) parent).getChildrenAt(last));
					((PlaylistNodePrimaryIndex) parent).getChildrenAt(last).setParent(new_int_node);

					Integer move_up_key = ((PlaylistNodePrimaryIndex) parent).audioIdAtIndex(parent_size/2);
					for(int i = (parent_size-1); i>=(parent_size/2); i--){
						((PlaylistNodePrimaryIndex) parent).get_audioIds().remove(i);
					}

					Integer children_size = ((PlaylistNodePrimaryIndex) parent).getAllChildren().size();
					for(int i = (children_size-1); i>=(children_size/2); i--){
						((PlaylistNodePrimaryIndex) parent).getAllChildren().remove(i);
					}

					new_root.add_audioId(0, move_up_key);
					new_root.getAllChildren().add(0, parent);
					parent.setParent(new_root);
					new_root.getAllChildren().add(1, new_int_node);
					primaryRoot = new_root;
					break;
				}

				// else if overflowed parent is an internal node
				else if(parent.getParent() != null){
					PlaylistNodePrimaryIndex new_int_node = new PlaylistNodePrimaryIndex(parent.getParent());

					//now move up instead of copy up
					Integer parent_size = ((PlaylistNodePrimaryIndex) parent).audioIdCount();
					Integer j=0;
					for(int i=(parent_size/2+1); i<parent_size; i++){
						new_int_node.add_audioId(j, ((PlaylistNodePrimaryIndex) parent).audioIdAtIndex(i));
						new_int_node.getAllChildren().add(j, ((PlaylistNodePrimaryIndex) parent).getChildrenAt(i));
						((PlaylistNodePrimaryIndex) parent).getChildrenAt(i).setParent(new_int_node);
						j++;
					}
					Integer last = ((PlaylistNodePrimaryIndex) parent).audioIdCount();
					new_int_node.getAllChildren().add(new_int_node.audioIdCount(), ((PlaylistNodePrimaryIndex) parent).getChildrenAt(last));
					((PlaylistNodePrimaryIndex) parent).getChildrenAt(last).setParent(new_int_node);

					Integer move_up_key = ((PlaylistNodePrimaryIndex) parent).audioIdAtIndex(parent_size/2);
					for(int i = (parent_size-1); i>=(parent_size/2); i--){
						((PlaylistNodePrimaryIndex) parent).get_audioIds().remove(i);
					}

					Integer children_size = ((PlaylistNodePrimaryIndex) parent).getAllChildren().size();
					for(int i = (children_size-1); i>=(children_size/2); i--){
						((PlaylistNodePrimaryIndex) parent).getAllChildren().remove(i);
					}

					// we should add new internal node to its parent
					index = -1;
					for(int i=0; i<((PlaylistNodePrimaryIndex)parent.getParent()).audioIdCount(); i++){
						if(move_up_key < ((PlaylistNodePrimaryIndex)parent.getParent()).audioIdAtIndex(i)){
							((PlaylistNodePrimaryIndex)parent.getParent()).add_audioId(i, move_up_key);
							((PlaylistNodePrimaryIndex)parent.getParent()).getAllChildren().set(i, parent);
							((PlaylistNodePrimaryIndex)parent.getParent()).getAllChildren().add(i+1, new_int_node);
							index = i;
							break;
						}
					}

					if(index == -1){
						((PlaylistNodePrimaryIndex)parent.getParent()).add_audioId(move_up_key);
						((PlaylistNodePrimaryIndex)parent.getParent()).getAllChildren().add(new_int_node);
					}

					parent_next = parent.getParent();
					parent = parent_next;
				}
			}
		}
	}

	public void add_secondary(CengSong song){
		// Secondary Tree
		// Find the correct leaf to insert song
		PlaylistNodeSecondaryLeaf leaf_to_add_sec = node_to_insert_secondary(song.genre());
		ArrayList<ArrayList<CengSong>> song_bucket_at_leaf = leaf_to_add_sec.getSongBucket();
		Integer index_sec = -1;

		// if initially song_bucket_at_leaf is not empty
		if(song_bucket_at_leaf.size() != 0) {
			for (int i = 0; i < song_bucket_at_leaf.size(); i++) {
				// if song.genre < genre at that index
				if (song.genre().compareTo(leaf_to_add_sec.genreAtIndex(i)) == 0) {
					//song_bucket_at_leaf.add(index_sec, new ArrayList<CengSong>());
					index_sec = i;
					break;
				}
				else if (song.genre().compareTo(leaf_to_add_sec.genreAtIndex(i)) < 0) {
					index_sec = i;
					song_bucket_at_leaf.add(index_sec, new ArrayList<CengSong>());
					break;
				}
			}
		}
		// bu ife ya initially boşsa ya da son indexe gelmesi gerekiyorsa girecek.
		if(index_sec == -1){
			index_sec = song_bucket_at_leaf.size();
			song_bucket_at_leaf.add(new ArrayList<CengSong>());
		}

		// add song to the leaf
		song_bucket_at_leaf.get(index_sec).add(song);

		Integer size_sec = song_bucket_at_leaf.size();

		// if no overflow
		if(size_sec <= (2*PlaylistNode.order)){
			return ;
		}

		// if overflow at leaf && leaf_to_add_sec == root
		else if(leaf_to_add_sec.getParent() == null){
			//copy up
			PlaylistNodeSecondaryIndex new_root = new PlaylistNodeSecondaryIndex(null);
			PlaylistNodeSecondaryLeaf new_node = new PlaylistNodeSecondaryLeaf(new_root);
			Integer j=0;
			for(int i = (size_sec/2); i<size_sec; i++){
				new_node.getSongBucket().add(j, new ArrayList<CengSong>());
				new_node.getSongBucket().set(j, leaf_to_add_sec.getSongBucket().get(i));
				j++;
			}
			for(int i = size_sec-1; i>=size_sec/2; i--){
				leaf_to_add_sec.getSongBucket().remove(i);
			}
			leaf_to_add_sec.setParent(new_root);
			new_root.add_genre(0, new_node.genreAtIndex(0));
			new_root.getAllChildren().add(0, leaf_to_add_sec);
			new_root.getAllChildren().add(1, new_node);
			secondaryRoot = new_root;

		}

		// else if overflow at leaf && leaf_to_add_sec != root
		else if(leaf_to_add_sec.getParent() != null) {
			PlaylistNode parent = leaf_to_add_sec.getParent();
			PlaylistNodeSecondaryLeaf new_node = new PlaylistNodeSecondaryLeaf(parent);
			Integer k = 0;
			for (int i = (size_sec / 2); i < size_sec; i++) {
				new_node.getSongBucket().add(k, new ArrayList<CengSong>());
				new_node.getSongBucket().set(k, leaf_to_add_sec.getSongBucket().get(i));
				k++;
			}
			for (int i = size_sec - 1; i >= size_sec / 2; i--) {
				leaf_to_add_sec.getSongBucket().remove(i);
			}

			// copy up the key to its place
			String copy_up_genre = new_node.genreAtIndex(0);
			index_sec = -1;
			for (int i = 0; i < ((PlaylistNodeSecondaryIndex) parent).genreCount(); i++) {
				if (copy_up_genre.compareTo((((PlaylistNodeSecondaryIndex) parent).genreAtIndex(i))) < 0) {
					((PlaylistNodeSecondaryIndex) parent).add_genre(i, copy_up_genre);
					((PlaylistNodeSecondaryIndex) parent).getAllChildren().add(i + 1, new_node);
					index_sec = i;
					break;
				}
			}
			if (index_sec == -1) {
				((PlaylistNodeSecondaryIndex) parent).add_genre(copy_up_genre);
				((PlaylistNodeSecondaryIndex) parent).getAllChildren().add(new_node);
			}


			// check if overflow occurs at parent when we copy up
			while (((PlaylistNodeSecondaryIndex) parent).genreCount() > (2 * PlaylistNode.order)) {
				PlaylistNode parent_next;

				// if overflowed parent is root
				if (parent.getParent() == null) {
					PlaylistNodeSecondaryIndex new_root = new PlaylistNodeSecondaryIndex(null);
					PlaylistNodeSecondaryIndex new_int_node = new PlaylistNodeSecondaryIndex(new_root);

					//now move up instead of copy up
					Integer parent_size = ((PlaylistNodeSecondaryIndex) parent).genreCount();
					Integer j = 0;
					for (int i = (parent_size / 2 + 1); i < parent_size; i++) {
						new_int_node.add_genre(j, ((PlaylistNodeSecondaryIndex) parent).genreAtIndex(i));
						new_int_node.getAllChildren().add(j, ((PlaylistNodeSecondaryIndex) parent).getChildrenAt(i));
						((PlaylistNodeSecondaryIndex) parent).getChildrenAt(i).setParent(new_int_node);
						j++;
					}
					Integer last = ((PlaylistNodeSecondaryIndex) parent).genreCount();
					new_int_node.getAllChildren().add(new_int_node.genreCount(), ((PlaylistNodeSecondaryIndex) parent).getChildrenAt(last));
					((PlaylistNodeSecondaryIndex) parent).getChildrenAt(last).setParent(new_int_node);

					String move_up_genre = ((PlaylistNodeSecondaryIndex) parent).genreAtIndex(parent_size / 2);
					for (int i = (parent_size - 1); i >= (parent_size / 2); i--) {
						((PlaylistNodeSecondaryIndex) parent).get_genres().remove(i);
					}

					Integer children_size = ((PlaylistNodeSecondaryIndex) parent).getAllChildren().size();
					for (int i = (children_size - 1); i >= (children_size / 2); i--) {
						((PlaylistNodeSecondaryIndex) parent).getAllChildren().remove(i);
					}

					new_root.add_genre(0, move_up_genre);
					new_root.getAllChildren().add(0, parent);
					parent.setParent(new_root);
					new_root.getAllChildren().add(1, new_int_node);
					secondaryRoot = new_root;
					break;
				}

				// else if overflowed parent is an internal node

				else if(parent.getParent() != null){
					PlaylistNodeSecondaryIndex new_int_node = new PlaylistNodeSecondaryIndex(parent.getParent());

					//now move up instead of copy up
					Integer parent_size = ((PlaylistNodeSecondaryIndex) parent).genreCount();
					Integer j=0;
					for(int i=(parent_size/2+1); i<parent_size; i++){
						new_int_node.add_genre(j, ((PlaylistNodeSecondaryIndex) parent).genreAtIndex(i));
						new_int_node.getAllChildren().add(j, ((PlaylistNodeSecondaryIndex) parent).getChildrenAt(i));
						((PlaylistNodeSecondaryIndex) parent).getChildrenAt(i).setParent(new_int_node);
						j++;
					}
					Integer last = ((PlaylistNodeSecondaryIndex) parent).genreCount();
					new_int_node.getAllChildren().add(new_int_node.genreCount(), ((PlaylistNodeSecondaryIndex) parent).getChildrenAt(last));
					((PlaylistNodeSecondaryIndex) parent).getChildrenAt(last).setParent(new_int_node);

					String move_up_genre = ((PlaylistNodeSecondaryIndex) parent).genreAtIndex(parent_size/2);
					for(int i = (parent_size-1); i>=(parent_size/2); i--){
						((PlaylistNodeSecondaryIndex) parent).get_genres().remove(i);
					}

					Integer children_size = ((PlaylistNodeSecondaryIndex) parent).getAllChildren().size();
					for(int i = (children_size-1); i>=(children_size/2); i--){
						((PlaylistNodeSecondaryIndex) parent).getAllChildren().remove(i);
					}

					// we should add new internal node to its parent
					index_sec = -1;
					for(int i=0; i<((PlaylistNodeSecondaryIndex)parent.getParent()).genreCount(); i++){
						if(move_up_genre.compareTo(((PlaylistNodeSecondaryIndex)parent.getParent()).genreAtIndex(i)) < 0){
							((PlaylistNodeSecondaryIndex)parent.getParent()).add_genre(i, move_up_genre);
							((PlaylistNodeSecondaryIndex)parent.getParent()).getAllChildren().set(i, parent);
							((PlaylistNodeSecondaryIndex)parent.getParent()).getAllChildren().add(i+1, new_int_node);
							index_sec = i;
							break;
						}
					}

					if(index_sec == -1){
						((PlaylistNodeSecondaryIndex)parent.getParent()).add_genre(move_up_genre);
						((PlaylistNodeSecondaryIndex)parent.getParent()).getAllChildren().add(new_int_node);
					}

					parent_next = parent.getParent();
					parent = parent_next;
				}


			}

		}
	}
	public void addSong(CengSong song) {

		add_primary(song);
		add_secondary(song);

		return;
	}

	// Search
	public CengSong searchSong(Integer audioId) {
		// find the song with the searched audioId in primary B+ tree
		// return value will not be tested, just print according to the specifications
		String indent = "";
		PlaylistNode node = primaryRoot;
		boolean is_greater = false;
		while(node.getType() != PlaylistNodeType.Leaf){

			System.out.println(indent + "<index>");
			for (int i = 0; i < ((PlaylistNodePrimaryIndex) node).audioIdCount(); i++) {
				System.out.println(indent + ((PlaylistNodePrimaryIndex) node).audioIdAtIndex(i));
			}
			System.out.println(indent + "</index>");
			is_greater = false;
			for (int i = 0; i < ((PlaylistNodePrimaryIndex) node).audioIdCount(); i++) {
				// if ith key is greater than our key, then i is the index of next child
				if (((PlaylistNodePrimaryIndex) node).audioIdAtIndex(i) > audioId)
				{
					node = ((PlaylistNodePrimaryIndex) node).getChildrenAt(i);
					is_greater = true;
					break;
				}
			}
			if (is_greater == false) {
				node = ((PlaylistNodePrimaryIndex) node).getChildrenAt(((PlaylistNodePrimaryIndex) node).audioIdCount());
			}
			indent = indent + "\t";
		}
		for (int i = 0; i < ((PlaylistNodePrimaryLeaf) node).songCount(); i++) {
			if (((PlaylistNodePrimaryLeaf) node).songAtIndex(i).audioId() == audioId)
			{
				System.out.println(indent + "<data>");
				System.out.print(indent + "<record>");
				System.out.print(((PlaylistNodePrimaryLeaf) node).songAtIndex(i).audioId());
				System.out.print("|");
				System.out.print(((PlaylistNodePrimaryLeaf) node).songAtIndex(i).genre());
				System.out.print("|");
				System.out.print(((PlaylistNodePrimaryLeaf) node).songAtIndex(i).songName());
				System.out.print("|");
				System.out.print(((PlaylistNodePrimaryLeaf) node).songAtIndex(i).artist());
				System.out.println("</record>");
				System.out.println(indent + "</data>");
				return (((PlaylistNodePrimaryLeaf) node).songAtIndex(i));
			}
		}
		System.out.println("Could not find " + audioId + ".");
		return null;
	}
	
	public void print_primary_helper(PlaylistNode node, int level){
		if(node.getType() == PlaylistNodeType.Internal){
			for (int i = 0; i < level; i++) {
				System.out.print("\t");
			}
			System.out.println("<index>");

			for (int i = 0; i < ((PlaylistNodePrimaryIndex) node).audioIdCount(); i++) {
				for (int j = 0; j < level; j++) {
					System.out.print("\t");
				}
				System.out.println(((PlaylistNodePrimaryIndex) node).audioIdAtIndex(i));
			}

			for (int i = 0; i < level; i++) {
				System.out.print("\t");
			}
			System.out.println("</index>");

			for(int i=0; i<((PlaylistNodePrimaryIndex) node).getAllChildren().size(); i++){
				print_primary_helper(((PlaylistNodePrimaryIndex) node).getAllChildren().get(i), level+1);
			}
		}

		else if(node.getType() == PlaylistNodeType.Leaf){
			for (int i = 0; i < level; i++) {
				System.out.print("\t");
			}
			System.out.println("<data>");

			for (int i = 0; i < ((PlaylistNodePrimaryLeaf) node).songCount() ; i++){
				for (int j = 0; j < level; j++) {
					System.out.print("\t");
				}
				System.out.print("<record>");
				System.out.print(((PlaylistNodePrimaryLeaf) node).songAtIndex(i).audioId());
				System.out.print("|");
				System.out.print(((PlaylistNodePrimaryLeaf) node).songAtIndex(i).genre());
				System.out.print("|");
				System.out.print(((PlaylistNodePrimaryLeaf) node).songAtIndex(i).songName());
				System.out.print("|");
				System.out.print(((PlaylistNodePrimaryLeaf) node).songAtIndex(i).artist());
				System.out.print("</record>\n");
			}

			for (int i = 0; i < level; i++) {
				System.out.print("\t");
			}
			System.out.println("</data>");
		}
	}

	public void print_secondary_helper(PlaylistNode node, int level){
		if(node.getType() == PlaylistNodeType.Internal){
			for (int i = 0; i < level; i++) {
				System.out.print("\t");
			}
			System.out.println("<index>");

			for (int i = 0; i < ((PlaylistNodeSecondaryIndex) node).genreCount(); i++) {
				for (int j = 0; j < level; j++) {
					System.out.print("\t");
				}
				System.out.println(((PlaylistNodeSecondaryIndex) node).genreAtIndex(i));
			}

			for (int i = 0; i < level; i++) {
				System.out.print("\t");
			}
			System.out.println("</index>");

			for(int i=0; i<((PlaylistNodeSecondaryIndex) node).getAllChildren().size(); i++){
				print_secondary_helper(((PlaylistNodeSecondaryIndex) node).getAllChildren().get(i), level+1);
			}
		}

		else if(node.getType() == PlaylistNodeType.Leaf){
			for (int i = 0; i < level; i++) {
				System.out.print("\t");
			}
			System.out.println("<data>");

			for (int i = 0; i < ((PlaylistNodeSecondaryLeaf) node).getSongBucket().size() ; i++){
				for (int j = 0; j < level; j++) {
					System.out.print("\t");
				}
				System.out.println(((PlaylistNodeSecondaryLeaf)node).genreAtIndex(i));
				for(int k=0; k<((PlaylistNodeSecondaryLeaf) node).getSongBucket().get(i).size(); k++) {
					for (int j = 0; j < level + 1; j++) {
						System.out.print("\t");
					}
					System.out.print("<record>");
					System.out.print(((PlaylistNodeSecondaryLeaf) node).getSongBucket().get(i).get(k).audioId());
					System.out.print("|");
					System.out.print(((PlaylistNodeSecondaryLeaf) node).getSongBucket().get(i).get(k).genre());
					System.out.print("|");
					System.out.print(((PlaylistNodeSecondaryLeaf) node).getSongBucket().get(i).get(k).songName());
					System.out.print("|");
					System.out.print(((PlaylistNodeSecondaryLeaf) node).getSongBucket().get(i).get(k).artist());
					System.out.print("</record>\n");
				}
			}

			for (int i = 0; i < level; i++) {
				System.out.print("\t");
			}
			System.out.println("</data>");
		}
	}

	public void printPrimaryPlaylist() {
		PlaylistNode root = primaryRoot;
		print_primary_helper(root, 0);
		return;
	}
	public void printSecondaryPlaylist() {
		// print the secondary B+ tree in Depth-first order
		PlaylistNode root = secondaryRoot;
		print_secondary_helper(root, 0);
		return;
	}
	
	// Extra functions if needed

}


