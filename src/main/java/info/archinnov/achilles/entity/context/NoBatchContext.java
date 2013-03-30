package info.archinnov.achilles.entity.context;

import info.archinnov.achilles.dao.CounterDao;
import info.archinnov.achilles.dao.GenericCompositeDao;
import info.archinnov.achilles.dao.GenericDynamicCompositeDao;

import java.util.Map;

/**
 * NoBatchContext
 * 
 * @author DuyHai DOAN
 * 
 */
public class NoBatchContext extends AbstractBatchContext
{

	/**
	 * @param entityDaosMap
	 * @param columnFamilyDaosMap
	 * @param counterDao
	 */
	public NoBatchContext(Map<String, GenericDynamicCompositeDao<?>> entityDaosMap,
			Map<String, GenericCompositeDao<?, ?>> columnFamilyDaosMap, CounterDao counterDao)
	{
		super(entityDaosMap, columnFamilyDaosMap, counterDao);
	}

	@Override
	public <ID> void flush()
	{
		doFlush();
	}

	@Override
	public void endBatch()
	{
		throw new UnsupportedOperationException(
				"The method 'endBatch()' is not supported for a NoBatchContext. Please use it within a batch context");

	}

	@Override
	public BatchType type()
	{
		return BatchType.NONE;
	}
}
