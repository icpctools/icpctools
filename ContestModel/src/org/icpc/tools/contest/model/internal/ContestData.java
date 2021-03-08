package org.icpc.tools.contest.model.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IContestListener.Delta;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IDelete;

/**
 * Unsynchronized contest object store, optimized for additions vs removals. Optimized toArray,
 * getById, and getByType.
 */
public class ContestData implements Iterable<IContestObject> {
	private static final int ARRAY_SIZE = 10000;
	private static final int NUM_ARRAYS = 200;
	private static final int NUM_TYPES = IContestObject.ContestType.values().length;

	// true to keep history (objects that have been updated or deleted & delete entries), or false
	// to only keep current
	private boolean keepHistory;

	// full object list (array of arrays) & current size
	private IContestObject[][] objs = new IContestObject[NUM_ARRAYS][];
	private Delta[][] deltas = new Delta[NUM_ARRAYS][];
	private int totalSize;

	// cache for each object type, index back into main list, and sizes per type
	class TypeCache {
		// cache of objects of this type
		IContestObject[] cache = null;

		// indexes back to main list
		int[] index = null;

		// number of objects of this type
		int size;

		// map of ids to local index for quick lookup
		Map<String, Integer> idMap = new HashMap<>();

		// boolean[] isUpdate;

		@Override
		public String toString() {
			return "Type cache: " + size;
		}
	}

	private TypeCache[] typeCache = new TypeCache[NUM_TYPES];

	// last copy of toArray for convenience
	private IContestObject[] toArray = null;

	public ContestData() {
		this(true);
	}

	public ContestData(boolean keepHistory) {
		this.keepHistory = keepHistory;

		for (int i = 0; i < NUM_TYPES; i++)
			typeCache[i] = new TypeCache();
	}

	public int size() {
		return totalSize;
	}

	public boolean isEmpty() {
		return totalSize == 0;
	}

	@Override
	public Iterator<IContestObject> iterator() {
		return new Iterator<IContestObject>() {
			private int n = 0;

			@Override
			public boolean hasNext() {
				return n < totalSize;
			}

			@Override
			public IContestObject next() {
				int arr = n % ARRAY_SIZE;
				int num = n / ARRAY_SIZE;
				n++;
				return objs[num][arr];
			}
		};
	}

	/**
	 * Add a contest object based on the keepHistory setting of this ContestData. Returns a
	 * DeltaType status to tell the client if this change resulted in an Add, Update, Delete, or
	 * No-op.
	 *
	 * @param obj
	 * @return
	 */
	public Delta add(IContestObject obj) {
		if (obj == null)
			return Delta.NOOP;

		int type = obj.getType().ordinal();
		TypeCache tc = typeCache[type];

		// look for existing object with the same type & id
		int index = -1;
		IContestObject current = null;
		if (tc.idMap != null) {
			Integer in = tc.idMap.get(obj.getId());
			if (in != null) {
				index = in;
				current = tc.cache[index];
			}
		}

		if (obj instanceof IDelete) {
			// can't delete an object that isn't there
			if (current == null)
				return Delta.NOOP;

			if (keepHistory) {
				deleteWhileKeepingHistory((Deletion) obj);
				return Delta.DELETE;
			}
			removeImpl(obj);
			return Delta.DELETE;
		}

		// can't update an object with no changes
		Delta d = Delta.ADD;
		if (current != null) {
			// check if anything has changed
			Map<String, Object> oldP = current.getProperties();
			Map<String, Object> newP = obj.getProperties();

			if (oldP.size() == newP.size()) {
				boolean changed = false;
				for (String oldK : oldP.keySet()) {
					if (!"time".equals(oldK)) {
						if ((oldP.get(oldK) == null && newP.get(oldK) == null)
								|| (oldP.get(oldK) != null && oldP.get(oldK).equals(newP.get(oldK)))) {
							// found match
							continue;
						}
						changed = true;
						break;
					}
				}

				if (!changed)
					return Delta.NOOP;
			}

			if (!keepHistory(obj)) {
				if (tc.cache != null) {
					tc.cache[index] = obj;
					set(tc.index[index], obj);
				}
				return Delta.UPDATE;
			}
			d = Delta.UPDATE;
		}
		addImpl(obj, index, d);
		return d;
	}

	private void deleteWhileKeepingHistory(Deletion obj) {
		int arr = totalSize % ARRAY_SIZE;
		int num = totalSize / ARRAY_SIZE;
		IContestObject[] co = objs[num];
		Delta[] delt = deltas[num];
		if (co == null) {
			co = new IContestObject[ARRAY_SIZE];
			objs[num] = co;
			delt = new Delta[ARRAY_SIZE];
			deltas[num] = delt;
		}
		co[arr] = obj;
		delt[arr] = Delta.DELETE;
		toArray = null;

		// remove the original from the type cache
		int type = obj.getType().ordinal();
		TypeCache tc = typeCache[type];

		int index = getIndexById(obj.getId(), type);
		if (index < tc.size - 1) {
			System.arraycopy(tc.cache, index + 1, tc.cache, index, tc.size - index - 1);
			System.arraycopy(tc.index, index + 1, tc.index, index, tc.size - index - 1);
		}
		tc.cache[tc.size - 1] = null;

		tc.size--;
		if (tc.idMap != null) {
			tc.idMap.remove(obj.getId());

			for (String key : tc.idMap.keySet()) {
				Integer in = tc.idMap.get(key);
				if (in > index)
					tc.idMap.put(key, in - 1);
			}
		}

		totalSize++;
	}

	/**
	 * Add an object directly to the end of the contest object list, either: 1) an object that's
	 * definitely new 2) an update to an object, and history is being kept 3) a deletion, and
	 * history is being kept.
	 *
	 * @param obj
	 * @return
	 */
	private void addImpl(IContestObject obj, int index, Delta d) {
		int arr = totalSize % ARRAY_SIZE;
		int num = totalSize / ARRAY_SIZE;
		IContestObject[] co = objs[num];
		Delta[] delt = deltas[num];
		if (co == null) {
			co = new IContestObject[ARRAY_SIZE];
			objs[num] = co;
			delt = new Delta[ARRAY_SIZE];
			deltas[num] = delt;
		}
		co[arr] = obj;
		delt[arr] = d;
		toArray = null;

		TypeCache tc = typeCache[obj.getType().ordinal()];

		// update case
		if (index >= 0) {
			tc.cache[index] = obj;
			tc.index[index] = totalSize;
			totalSize++;
			return;
		}

		// true add case
		if (tc.cache == null) {
			tc.cache = new IContestObject[20];
			tc.index = new int[20];
		} else if (tc.size == tc.cache.length) {
			IContestObject[] temp = new IContestObject[tc.cache.length * 3 / 2];
			System.arraycopy(tc.cache, 0, temp, 0, tc.cache.length);
			tc.cache = temp;

			int[] temp2 = new int[tc.index.length * 3 / 2];
			System.arraycopy(tc.index, 0, temp2, 0, tc.index.length);
			tc.index = temp2;
		}
		tc.cache[tc.size] = obj;
		tc.index[tc.size] = totalSize;

		if (tc.idMap == null)
			tc.idMap = new HashMap<>();
		tc.idMap.put(obj.getId(), tc.size);

		totalSize++;
		tc.size++;
	}

	public IContestObject[] toArray(ContestType cType) {
		TypeCache tc = typeCache[cType.ordinal()];
		int len = tc.size;
		IContestObject[] co = new IContestObject[len];
		if (len != 0)
			System.arraycopy(tc.cache, 0, co, 0, len);
		return co;
	}

	public <T extends IContestObject> T[] getByType(Class<T> typeCl, IContestObject.ContestType cType) {
		TypeCache tc = typeCache[cType.ordinal()];
		int len = tc.size;
		@SuppressWarnings("unchecked")
		final T[] co = (T[]) Array.newInstance(typeCl, len);
		if (len != 0)
			System.arraycopy(tc.cache, 0, co, 0, len);
		return co;
	}

	public IContestObject getById(String id, IContestObject.ContestType cType) {
		int index = getIndexById(id, cType);
		if (index == -1)
			return null;

		TypeCache tc = typeCache[cType.ordinal()];
		return tc.cache[index];
	}

	public int getIndexById(String id, IContestObject.ContestType cType) {
		if (id == null)
			return -1;

		int type = cType.ordinal();
		return getIndexById(id, type);
	}

	public int getIndexById(String id, int type) {
		TypeCache tc = typeCache[type];
		if (tc.idMap == null)
			return -1;

		Integer ind = tc.idMap.get(id);
		if (ind != null)
			return ind;
		return -1;
	}

	public IContestObject get(int index) {
		if (index < 0 || index >= totalSize)
			throw new IllegalArgumentException("Out of range");

		int arr = index % ARRAY_SIZE;
		int num = index / ARRAY_SIZE;
		return objs[num][arr];
	}

	private IContestObject set(int index, IContestObject obj) {
		if (index < 0 || index >= totalSize)
			throw new IllegalArgumentException("Out of range");

		int arr = index % ARRAY_SIZE;
		int num = index / ARRAY_SIZE;
		objs[num][arr] = obj;
		deltas[num][arr] = Delta.UPDATE;
		toArray = null;
		return obj;
	}

	public void remove(IContestObject obj) {
		if (keepHistory) {
			Deletion del = new Deletion(obj.getId(), obj.getType());
			deleteWhileKeepingHistory(del);
		} else
			removeImpl(obj);
	}

	/**
	 * Completely remove one object from all contest history. Not optimized for calling frequently,
	 * use removeFromHistory(List<IContestObject>) if removing many objects at once.
	 *
	 * @param obj the object to remove
	 */
	public void removeFromHistory(IContestObject obj) {
		removeImpl(obj);
	}

	/**
	 * Completely remove several objects from contest history.
	 *
	 * @param remove list of objects to remove
	 */
	public void removeFromHistory(List<IContestObject> remove) {
		// for each type, build a list of object ids to be removed
		@SuppressWarnings("unchecked")
		List<String>[] rtc = new List[NUM_TYPES];
		for (int i = 0; i < NUM_TYPES; i++)
			rtc[i] = new ArrayList<String>();

		for (IContestObject obj : remove) {
			int type = obj.getType().ordinal();
			rtc[type].add(obj.getId());
		}

		// null out entries in the main array
		for (int i = 0; i < totalSize; i++) {
			int arr = i % ARRAY_SIZE;
			int num = i / ARRAY_SIZE;
			IContestObject obj2 = objs[num][arr];
			if (obj2 != null && rtc[obj2.getType().ordinal()].contains(obj2.getId())) {
				objs[num][arr] = null;
				if (!keepHistory)
					break;
			}
		}

		// rebuild empty type caches
		for (int i = 0; i < NUM_TYPES; i++) {
			if (typeCache[i] != null) {
				int size = typeCache[i].size;
				TypeCache tc = new TypeCache();
				tc.cache = new IContestObject[size];
				tc.index = new int[size];
				typeCache[i] = tc;
			}
		}

		// build new main array, rebuilding type caches as we go
		totalSize = 0;
		IContestObject[][] objs2 = new IContestObject[NUM_ARRAYS][];
		Delta[][] deltas2 = new Delta[NUM_ARRAYS][];

		for (int num = 0; num < NUM_ARRAYS; num++) {
			if (objs[num] != null) {
				for (int arr = 0; arr < ARRAY_SIZE; arr++) {
					IContestObject obj = objs[num][arr];
					if (obj != null) {
						int arr2 = totalSize % ARRAY_SIZE;
						int num2 = totalSize / ARRAY_SIZE;

						IContestObject[] co2 = objs2[num2];
						Delta[] delt2 = deltas2[num2];
						if (co2 == null) {
							co2 = new IContestObject[ARRAY_SIZE];
							objs2[num2] = co2;
							delt2 = new Delta[ARRAY_SIZE];
							deltas2[num2] = delt2;
						}
						co2[arr2] = obj;
						delt2[arr2] = deltas[num][arr];

						// update type cache
						int index = -1;
						int type = obj.getType().ordinal();
						TypeCache tc = typeCache[type];
						Integer in = tc.idMap.get(obj.getId());
						if (in != null)
							index = in;

						if (index >= 0) {
							tc.cache[index] = obj;
							tc.index[index] = totalSize;
							totalSize++;
							continue;
						}

						tc.cache[tc.size] = obj;
						tc.index[tc.size] = totalSize;

						if (tc.idMap == null)
							tc.idMap = new HashMap<>();
						tc.idMap.put(obj.getId(), tc.size);

						totalSize++;
						tc.size++;
					}
				}
			}
		}
		objs = objs2;
		deltas = deltas2;
		toArray = null;
	}

	public void removeSince(int num) {
		if (keepHistory)
			throw new RuntimeException("Can't delete without harming history");

		for (int i = size(); i > num; i--)
			removeImpl(get(i));
	}

	/**
	 * Remove all history of the given object from the data model. (keep history = false)
	 *
	 * @param obj
	 * @param index
	 */
	private void removeImpl(IContestObject obj) {
		int type = obj.getType().ordinal();
		String id = obj.getId();

		TypeCache tc = typeCache[type];
		if (tc.idMap == null || !tc.idMap.containsKey(id))
			throw new IllegalArgumentException("Attempt to remove an object that doesn't exist: " + obj);

		// start by removing from the id map and getting the master index
		int tcIndex = tc.idMap.get(id);
		tc.idMap.remove(id);

		// remove from type cache, type cache index, and cache sizes
		if (tcIndex < tc.size - 1) {
			System.arraycopy(tc.cache, tcIndex + 1, tc.cache, tcIndex, tc.size - tcIndex - 1);
			System.arraycopy(tc.index, tcIndex + 1, tc.index, tcIndex, tc.size - tcIndex - 1);
		}
		tc.size--;

		for (String key : tc.idMap.keySet()) {
			Integer in = tc.idMap.get(key);
			if (in > tcIndex) {
				tc.idMap.put(key, in - 1);
			}
		}

		// remove all history from main array
		int ind = 0;
		while (ind < totalSize) {
			int arr = ind % ARRAY_SIZE;
			int num = ind / ARRAY_SIZE;
			IContestObject obj2 = objs[num][arr];
			if (obj2.getType().ordinal() == type && obj2.getId().equals(id))
				removeData(ind, tc);
			else
				ind++;
		}

		toArray = null;
	}

	private void removeData(int index, TypeCache tc) {
		int arr = index % ARRAY_SIZE;
		int num = index / ARRAY_SIZE;
		System.arraycopy(objs[num], arr + 1, objs[num], arr, ARRAY_SIZE - arr - 1);
		System.arraycopy(deltas[num], arr + 1, deltas[num], arr, ARRAY_SIZE - arr - 1);
		if (objs[num + 1] != null) {
			objs[num][ARRAY_SIZE - 1] = objs[num + 1][0];
			deltas[num][ARRAY_SIZE - 1] = deltas[num + 1][0];
		}

		for (int i = num + 1; i < NUM_ARRAYS; i++) {
			if (objs[i] != null) {
				System.arraycopy(objs[i], 1, objs[i], 0, ARRAY_SIZE - 1);
				System.arraycopy(deltas[i], 1, deltas[i], 0, ARRAY_SIZE - 1);
			}
			if (i + 1 < NUM_ARRAYS && objs[i + 1] != null) {
				objs[i][ARRAY_SIZE - 1] = objs[i + 1][0];
				deltas[i][ARRAY_SIZE - 1] = deltas[i + 1][0];
			}
		}

		// fix type cache references
		for (TypeCache tc2 : typeCache) {
			for (int i = 0; i < tc2.size; i++) {
				if (tc2.index[i] > index)
					tc2.index[i]--;
			}
		}

		totalSize--;
	}

	public void clone(ContestData list) {
		toArray = null;

		for (int i = 0; i < NUM_ARRAYS; i++) {
			if (list.objs[i] != null) {
				objs[i] = new IContestObject[list.objs[i].length];
				System.arraycopy(list.objs[i], 0, objs[i], 0, list.objs[i].length);
				deltas[i] = new Delta[list.deltas[i].length];
				System.arraycopy(list.deltas[i], 0, deltas[i], 0, list.deltas[i].length);
			}
		}
		totalSize = list.totalSize;

		for (int i = 0; i < NUM_TYPES; i++) {
			TypeCache tc = typeCache[i];
			TypeCache ltc = list.typeCache[i];

			tc.size = ltc.size;

			if (ltc.cache != null) {
				tc.cache = new IContestObject[tc.size];
				System.arraycopy(ltc.cache, 0, tc.cache, 0, tc.size);
			}

			if (ltc.index != null) {
				tc.index = new int[tc.size];
				System.arraycopy(ltc.index, 0, tc.index, 0, tc.size);
			}

			if (ltc.idMap != null) {
				tc.idMap = new HashMap<>();

				for (String key : ltc.idMap.keySet()) {
					tc.idMap.put(key, ltc.idMap.get(key));
				}
			}
		}
	}

	public void iterate(IContest contest, IContestListener listener) {
		for (int i = 0; i < totalSize; i++) {
			int arr = i % ARRAY_SIZE;
			int num = i / ARRAY_SIZE;
			listener.contestChanged(contest, objs[num][arr], deltas[num][arr]);
		}
	}

	private boolean keepHistory(IContestObject obj) {
		return keepHistory || isConfigElement(obj);
	}

	private static boolean isConfigElement(IContestObject obj) {
		ContestType type = obj.getType();
		return (type == ContestType.CONTEST || type == ContestType.PROBLEM || type == ContestType.GROUP
				|| type == ContestType.LANGUAGE || type == ContestType.JUDGEMENT_TYPE || type == ContestType.TEAM
				|| type == ContestType.TEAM_MEMBER || type == ContestType.ORGANIZATION || type == ContestType.STATE);
	}

	public IContestObject[] toArray() {
		if (toArray != null)
			return toArray;

		toArray = new IContestObject[totalSize];
		int ind = 0;
		int i = 0;
		while (ind <= totalSize - ARRAY_SIZE) {
			System.arraycopy(objs[i], 0, toArray, ind, ARRAY_SIZE);
			ind += ARRAY_SIZE;
			i++;
		}
		if (objs[i] != null)
			System.arraycopy(objs[i], 0, toArray, ind, totalSize - ind);
		return toArray;
	}

	public void listByType() {
		System.out.println("Count by type:");
		for (IContestObject.ContestType ct : IContestObject.ContestType.values()) {
			System.out.println("  " + ct.name() + ": " + typeCache[ct.ordinal()].size);
		}
	}

	@Override
	public String toString() {
		return "Contest data [" + totalSize + "]";
	}
}