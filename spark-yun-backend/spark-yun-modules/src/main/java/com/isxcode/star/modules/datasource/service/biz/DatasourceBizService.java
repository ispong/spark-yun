package com.isxcode.star.modules.datasource.service.biz;

import com.isxcode.star.api.datasource.constants.DatasourceStatus;
import com.isxcode.star.api.datasource.constants.DatasourceType;
import com.isxcode.star.api.datasource.pojos.req.*;
import com.isxcode.star.api.datasource.pojos.res.*;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;
import com.isxcode.star.backend.api.base.properties.IsxAppProperties;
import com.isxcode.star.common.utils.AesUtils;
import com.isxcode.star.common.utils.path.PathUtils;
import com.isxcode.star.modules.datasource.entity.DatabaseDriverEntity;
import com.isxcode.star.modules.datasource.entity.DatasourceEntity;
import com.isxcode.star.modules.datasource.mapper.DatasourceMapper;
import com.isxcode.star.modules.datasource.repository.DatabaseDriverRepository;
import com.isxcode.star.modules.datasource.repository.DatasourceRepository;
import com.isxcode.star.modules.datasource.service.DatabaseDriverService;
import com.isxcode.star.modules.datasource.service.DatasourceService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static com.isxcode.star.common.config.CommonConfig.JPA_TENANT_MODE;
import static com.isxcode.star.common.config.CommonConfig.TENANT_ID;
import static com.isxcode.star.modules.datasource.service.DatasourceService.ALL_EXIST_DRIVER;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DatasourceBizService {

	private final DatasourceRepository datasourceRepository;

	private final DatasourceMapper datasourceMapper;

	private final AesUtils aesUtils;

	private final DatasourceService datasourceService;

	private final DatabaseDriverRepository databaseDriverRepository;

	private final IsxAppProperties isxAppProperties;

	private final DatabaseDriverService databaseDriverService;

	public void addDatasource(AddDatasourceReq addDatasourceReq) {

		DatasourceEntity datasource = datasourceMapper.dasAddDatasourceReqToDatasourceEntity(addDatasourceReq);

		// 密码对成加密
		datasource.setPasswd(aesUtils.encrypt(datasource.getPasswd()));

		// 判断如果是hive数据源，metastore_uris没有填写，附加默认值，thrift://localhost:9083
		if (DatasourceType.HIVE.equals(addDatasourceReq.getDbType())
				&& Strings.isEmpty(addDatasourceReq.getMetastoreUris())) {
			datasource.setMetastoreUris("thrift://localhost:9083");
		}

		datasource.setCheckDateTime(LocalDateTime.now());
		datasource.setStatus(DatasourceStatus.UN_CHECK);
		datasourceRepository.save(datasource);
	}

	public void updateDatasource(UpdateDatasourceReq updateDatasourceReq) {

		DatasourceEntity datasource = datasourceService.getDatasource(updateDatasourceReq.getId());

		datasource = datasourceMapper.dasUpdateDatasourceReqToDatasourceEntity(updateDatasourceReq, datasource);

		// 密码对成加密
		datasource.setPasswd(aesUtils.encrypt(datasource.getPasswd()));

		// 判断如果是hive数据源，metastore_uris没有填写，附加默认值，thrift://localhost:9083
		if (DatasourceType.HIVE.equals(updateDatasourceReq.getDbType())
				&& Strings.isEmpty(updateDatasourceReq.getMetastoreUris())) {
			datasource.setMetastoreUris("thrift://localhost:9083");
		}

		datasource.setCheckDateTime(LocalDateTime.now());
		datasource.setStatus(DatasourceStatus.UN_CHECK);
		datasourceRepository.save(datasource);
	}

	public Page<PageDatasourceRes> pageDatasource(PageDatasourceReq dasQueryDatasourceReq) {

		Page<DatasourceEntity> datasourceEntityPage = datasourceRepository.searchAll(
				dasQueryDatasourceReq.getSearchKeyWord(),
				PageRequest.of(dasQueryDatasourceReq.getPage(), dasQueryDatasourceReq.getPageSize()));

    Page<PageDatasourceRes> pageDatasourceRes = datasourceMapper.datasourceEntityToQueryDatasourceResPage(datasourceEntityPage);
    pageDatasourceRes.getContent().forEach(e->{
      if (!Strings.isEmpty(e.getDriverId())) {
        e.setDriverName(databaseDriverRepository.findById(e.getDriverId()).get().getName());
      }
    });

    return pageDatasourceRes;
  }

	public void deleteDatasource(DeleteDatasourceReq deleteDatasourceReq) {

		datasourceRepository.deleteById(deleteDatasourceReq.getDatasourceId());
	}

	public TestConnectRes testConnect(GetConnectLogReq testConnectReq) {

		DatasourceEntity datasource = datasourceService.getDatasource(testConnectReq.getDatasourceId());

		// 测试连接
		datasource.setCheckDateTime(LocalDateTime.now());

		try (Connection connection = datasourceService.getDbConnection(datasource)) {
			if (connection != null) {
				datasource.setStatus(DatasourceStatus.ACTIVE);
				datasource.setConnectLog("测试连接成功！");
				datasourceRepository.save(datasource);
				return new TestConnectRes(true, "连接成功");
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			datasource.setStatus(DatasourceStatus.FAIL);
			datasource.setConnectLog("测试连接失败：" + e.getMessage());
			datasourceRepository.save(datasource);
			return new TestConnectRes(false, e.getMessage());
		}
		return new TestConnectRes(false, "连接失败");
	}

	public GetConnectLogRes getConnectLog(GetConnectLogReq getConnectLogReq) {

		DatasourceEntity datasource = datasourceService.getDatasource(getConnectLogReq.getDatasourceId());

		return GetConnectLogRes.builder().connectLog(datasource.getConnectLog()).build();
	}

	public void uploadDatabaseDriver(MultipartFile driverFile, String dbType, String name, String remark) {

		// 判断驱动文件夹是否存在，没有则创建
		String driverDirPath = PathUtils.parseProjectPath(isxAppProperties.getResourcesPath()) + File.separator + "jdbc"
				+ File.separator + TENANT_ID.get();
		if (!new File(driverDirPath).exists()) {
			try {
				Files.createDirectories(Paths.get(driverDirPath));
			} catch (IOException e) {
				throw new IsxAppException("上传驱动，目录创建失败");
			}
		}

		// 保存驱动文件
		try (InputStream inputStream = driverFile.getInputStream()) {
			Files.copy(inputStream, Paths.get(driverDirPath).resolve(driverFile.getOriginalFilename()),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new IsxAppException("上传许可证失败");
		}

		// 初始化驱动对象
		DatabaseDriverEntity databaseDriver = DatabaseDriverEntity.builder().name(name).dbType(dbType)
				.driverType("TENANT_DRIVER").remark(remark).isDefaultDriver(false)
				.fileName(driverFile.getOriginalFilename()).build();

		// 持久化
		databaseDriverRepository.save(databaseDriver);
	}

	public Page<PageDatabaseDriverRes> pageDatabaseDriver(PageDatabaseDriverReq pageDatabaseDriverReq) {

		JPA_TENANT_MODE.set(false);
		Page<DatabaseDriverEntity> pageDatabaseDriver = databaseDriverRepository.searchAll(
				pageDatabaseDriverReq.getSearchKeyWord(), TENANT_ID.get(),
				PageRequest.of(pageDatabaseDriverReq.getPage(), pageDatabaseDriverReq.getPageSize()));

		return datasourceMapper.dataDriverEntityToPageDatabaseDriverResPage(pageDatabaseDriver);
	}

	public void deleteDatabaseDriver(DeleteDatabaseDriverReq deleteDatabaseDriverReq) {

		// 支持查询所有的数据
		JPA_TENANT_MODE.set(false);
		DatabaseDriverEntity driver = databaseDriverService.getDriver(deleteDatabaseDriverReq.getDriverId());
		JPA_TENANT_MODE.set(true);

		// 系统驱动无法删除
		if ("SYSTEM_DRIVER".equals(driver.getDriverType())) {
			throw new IsxAppException("系统数据源驱动无法删除");
		}

		// 判断驱动是否被别人使用，使用则不能删除
		List<DatasourceEntity> allDrivers = datasourceRepository.findAllByDriverId(driver.getId());
		if (!allDrivers.isEmpty()) {
			throw new IsxAppException("有数据源已使用当前驱动，无法删除");
		}

		// 卸载Map中的驱动
		ALL_EXIST_DRIVER.remove(driver.getId());

		// 将文件名改名字 xxx.jar ${driverId}_xxx.jar_bak
		try {
			String jdbcDirPath = PathUtils.parseProjectPath(isxAppProperties.getResourcesPath()) + File.separator
					+ "jdbc" + File.separator + TENANT_ID.get();
			Files.copy(Paths.get(jdbcDirPath).resolve(driver.getFileName()),
					Paths.get(jdbcDirPath).resolve(driver.getId() + "_" + driver.getFileName() + "_bak"),
					StandardCopyOption.REPLACE_EXISTING);
			Files.delete(Paths.get(jdbcDirPath).resolve(driver.getFileName()));
		} catch (IOException e) {
			throw new IsxAppException("删除驱动文件异常");
		}

		// 删除数据库
		databaseDriverRepository.deleteById(driver.getId());
	}

	public void settingDefaultDatabaseDriver(SettingDefaultDatabaseDriverReq settingDefaultDatabaseDriverReq) {

		DatabaseDriverEntity driver = databaseDriverService.getDriver(settingDefaultDatabaseDriverReq.getDriverId());

		if (settingDefaultDatabaseDriverReq.getIsDefaultDriver()) {
			// 将租户中其他的同类型驱动，默认状态都改成false
			List<DatabaseDriverEntity> allDriver = databaseDriverRepository.findAllByDbType(driver.getDbType());
			allDriver.forEach(e -> e.setIsDefaultDriver(false));
			databaseDriverRepository.saveAll(allDriver);
		}

		driver.setIsDefaultDriver(settingDefaultDatabaseDriverReq.getIsDefaultDriver());
		databaseDriverRepository.save(driver);
	}

	public GetDefaultDatabaseDriverRes getDefaultDatabaseDriver(
			GetDefaultDatabaseDriverReq getDefaultDatabaseDriverReq) {

		// 先查询租户的如果有直接返回
		Optional<DatabaseDriverEntity> defaultDriver = databaseDriverRepository
				.findByDriverTypeAndDbTypeAndIsDefaultDriver("TENANT_DRIVER", getDefaultDatabaseDriverReq.getDbType(),
						true);
		if (defaultDriver.isPresent()) {
			return datasourceMapper.databaseDriverEntityToGetDefaultDatabaseDriverRes(defaultDriver.get());
		}

		// 查询系统默认的返回
		JPA_TENANT_MODE.set(false);
		Optional<DatabaseDriverEntity> systemDriver = databaseDriverRepository
				.findByDriverTypeAndDbTypeAndIsDefaultDriver("SYSTEM_DRIVER", getDefaultDatabaseDriverReq.getDbType(),
						true);
		return datasourceMapper.databaseDriverEntityToGetDefaultDatabaseDriverRes(systemDriver.get());
	}

}
