/*
 * Copyright 2013, 2014 EnergyOS.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.energyos.espi.datacustodian.web.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.energyos.espi.common.domain.MeterReading;
import org.energyos.espi.common.domain.Routes;
import org.energyos.espi.common.domain.UsagePoint;
import org.energyos.espi.common.service.ExportService;
import org.energyos.espi.common.service.MeterReadingService;
import org.energyos.espi.common.service.ResourceService;
import org.energyos.espi.common.service.RetailCustomerService;
import org.energyos.espi.common.service.UsagePointService;
import org.energyos.espi.common.utils.ExportFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.sun.syndication.io.FeedException;

@Controller
public class MeterReadingRESTController {

	@Autowired
	private MeterReadingService meterReadingService;
	@Autowired
	private UsagePointService usagePointService;
	@Autowired
	private RetailCustomerService retailCustomerService;
	@Autowired
	private ExportService exportService;

	@Autowired
	private ResourceService resourceService;

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public void handleGenericException() {
	}

	// ROOT RESTFul APIs
	//
	@RequestMapping(value = Routes.ROOT_METER_READING_COLLECTION, method = RequestMethod.GET, produces = "application/atom+xml")
	@ResponseBody
	public void index(HttpServletResponse response,
			@RequestParam Map<String, String> params) throws IOException,
			FeedException {

		exportService.exportMeterReadings(response.getOutputStream(),
				new ExportFilter(params));
	}

	@RequestMapping(value = Routes.ROOT_METER_READING_MEMBER, method = RequestMethod.GET, produces = "application/atom+xml")
	@ResponseBody
	public void show(HttpServletResponse response,
			@PathVariable Long meterReadingId,
			@RequestParam Map<String, String> params) throws IOException,
			FeedException {

		try {
			exportService.exportMeterReading(meterReadingId,
					response.getOutputStream(), new ExportFilter(params));
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@RequestMapping(value = Routes.ROOT_METER_READING_COLLECTION, method = RequestMethod.POST, consumes = "application/atom+xml", produces = "application/atom+xml")
	@ResponseBody
	public void create(HttpServletResponse response,
			@RequestParam Map<String, String> params, InputStream stream)
			throws IOException {

		try {
			MeterReading meterReading = this.meterReadingService
					.importResource(stream);
			exportService.exportMeterReading(meterReading.getId(),
					response.getOutputStream(), new ExportFilter(params));
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	@RequestMapping(value = Routes.ROOT_METER_READING_MEMBER, method = RequestMethod.PUT, consumes = "application/atom+xml", produces = "application/atom+xml")
	@ResponseBody
	public void update(HttpServletResponse response,
			@PathVariable Long meterReadingId,
			@RequestParam Map<String, String> params, InputStream stream) {
		MeterReading meterReading = meterReadingService
				.findById(meterReadingId);
		if (meterReading != null) {
			try {
				MeterReading newMeterReading = meterReadingService
						.importResource(stream);
				meterReading.merge(newMeterReading);
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		}
	}

	@RequestMapping(value = Routes.ROOT_METER_READING_MEMBER, method = RequestMethod.DELETE)
	public void delete(HttpServletResponse response,
			@PathVariable Long meterReadingId) {
		try {
			resourceService.deleteById(meterReadingId, MeterReading.class);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	// XPath RESTFul APIs
	//
	@RequestMapping(value = Routes.METER_READING_COLLECTION, method = RequestMethod.GET, produces = "application/atom+xml")
	@ResponseBody
	public void index(HttpServletResponse response,
			@PathVariable Long retailCustomerId,
			@PathVariable Long usagePointId,
			@RequestParam Map<String, String> params) throws IOException,
			FeedException {

		exportService.exportMeterReadings(retailCustomerId, usagePointId,
				response.getOutputStream(), new ExportFilter(params));
	}

	@RequestMapping(value = Routes.METER_READING_MEMBER, method = RequestMethod.GET, produces = "application/atom+xml")
	@ResponseBody
	public void show(HttpServletResponse response,
			@PathVariable Long retailCustomerId,
			@PathVariable Long usagePointId, @PathVariable Long meterReadingId,
			@RequestParam Map<String, String> params) throws IOException,
			FeedException {

		try {
			exportService.exportMeterReading(retailCustomerId, usagePointId,
					meterReadingId, response.getOutputStream(),
					new ExportFilter(params));
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	//
	//
	@RequestMapping(value = Routes.METER_READING_COLLECTION, method = RequestMethod.POST, consumes = "application/atom+xml", produces = "application/atom+xml")
	@ResponseBody
	public void create(HttpServletResponse response,
			@PathVariable Long retailCustomerId,
			@PathVariable Long usagePointId,
			@RequestParam Map<String, String> params, InputStream stream)
			throws IOException {

		try {
			if (null != resourceService.findIdByXPath(retailCustomerId,
					usagePointId, UsagePoint.class)) {
				UsagePoint usagePoint = usagePointService
						.findById(usagePointId);
				MeterReading meterReading = meterReadingService
						.importResource(stream);
				meterReadingService.associateByUUID(usagePoint,
						meterReading.getUUID());
				exportService.exportMeterReading(retailCustomerId,
						usagePointId, meterReading.getId(),
						response.getOutputStream(), new ExportFilter(params));
			}
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	//
	@RequestMapping(value = Routes.METER_READING_MEMBER, method = RequestMethod.PUT, consumes = "application/atom+xml", produces = "application/atom+xml")
	@ResponseBody
	public void update(HttpServletResponse response,
			@PathVariable Long retailCustomerId,
			@PathVariable Long usagePointId, @PathVariable Long meterReadingId,
			@RequestParam Map<String, String> params, InputStream stream) {

		UsagePoint usagePoint = usagePointService.findById(usagePointId);
		MeterReading meterReading = meterReadingService.findById(
				retailCustomerId, usagePointId, meterReadingId);

		if (meterReading != null) {
			try {
				MeterReading newmeterReading = meterReadingService
						.importResource(stream);
				meterReadingService.associateByUUID(usagePoint,
						newmeterReading.getUUID());
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		}
	}

	@RequestMapping(value = Routes.METER_READING_MEMBER, method = RequestMethod.DELETE)
	public void delete(HttpServletResponse response,
			@PathVariable Long retailCustomerId,
			@PathVariable Long usagePointId, @PathVariable Long meterReadingId) {

		try {
			resourceService.deleteByXPathId(retailCustomerId, usagePointId,
					meterReadingId, MeterReading.class);

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	public void setMeterReadingService(MeterReadingService meterReadingService) {
		this.meterReadingService = meterReadingService;
	}

	public void setRetailCustomerService(
			RetailCustomerService retailCustomerService) {
		this.retailCustomerService = retailCustomerService;
	}

	public void setAtomService(MeterReadingService atomService) {
		this.meterReadingService = atomService;
	}

	public void setExportService(ExportService exportService) {
		this.exportService = exportService;
	}

	public void seResourceService(ResourceService resourceService) {
		this.resourceService = resourceService;
	}
}
