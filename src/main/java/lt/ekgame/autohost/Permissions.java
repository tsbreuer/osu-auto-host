package lt.ekgame.autohost;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Permissions {
	
	Set<Integer> operators = new HashSet<>();
	
	public Permissions(List<Integer> userIds) {
		operators.addAll(userIds);
	}
	
	public boolean addOperator(int osuId) {
		return operators.add(osuId);
	}
	
	public boolean isOperator(int osuId) {
		return operators.contains(osuId);
	}
}
