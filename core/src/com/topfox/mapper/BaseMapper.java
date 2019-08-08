package com.topfox.mapper;

import com.topfox.common.DataDTO;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import java.util.List;
import java.util.Map;

public interface BaseMapper<DTO extends DataDTO> {
	@InsertProvider(type = ProviderSql.class, method = "insertBySql")
	int insert(String sql);

	@DeleteProvider(type = ProviderSql.class, method = "deleteBySql")
	int delete(String sql);

	@UpdateProvider(type = ProviderSql.class, method = "updateBySql")
	int update(String sql);

	@UpdateProvider(type = ProviderSql.class, method = "updateBySql")
	int updateBatch(String sql);


	@SelectProvider(type = ProviderSql.class, method = "selectBySql")
	List<Map<String, Object>> selectMaps(String sql);

	@SelectProvider(type = ProviderSql.class, method = "selectBySql")
	int selectCount(String sql);

	@SelectProvider(type = ProviderSql.class, method = "selectBySql")
	Long selectForLong(String sql);

	@SelectProvider(type = ProviderSql.class, method = "selectBySql")
	List<DTO> listObjects(String sql);

	@SelectProvider(type = ProviderSql.class, method = "selectBySql")
	List<DTO> selectObjects(String sql);

	List<DTO> list(Map<String, Object> qto);
	List<DTO> listCount(Map<String, Object> qto);

}
