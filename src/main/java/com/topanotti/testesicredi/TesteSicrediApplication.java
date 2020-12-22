package com.topanotti.testesicredi;

import com.topanotti.testesicredi.dto.ContaDTO;
import com.topanotti.testesicredi.services.ReceitaService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.util.Iterator;

@SpringBootApplication
public class TesteSicrediApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(TesteSicrediApplication.class);
	private static ReceitaService receitaService;

	public TesteSicrediApplication(ReceitaService receitaService) {
		this.receitaService = receitaService;
	}

	public static void main(String[] args) {
		SpringApplication.run(TesteSicrediApplication.class, args);
		try {
			String fileDirectory = args[0];
			LOGGER.info("Diretorio do arquivo para a sincronizacao carregado com sucesso: " + fileDirectory);
			openFile(fileDirectory);
		} catch (ArrayIndexOutOfBoundsException ex) {
			LOGGER.error("É necessario informar o diretorio do arquivo de importacao.");
		}
	}

	public static void openFile(String fileName) {
		try {
			LOGGER.info("Realizando a abertura do arquivo: ");
			FileInputStream excelFile = new FileInputStream(new File(fileName));
			Workbook workbook = new XSSFWorkbook(excelFile);
			Sheet datatypeSheet = workbook.getSheetAt(0);
			readFileContent(datatypeSheet);
			saveNewFile(workbook);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void readFileContent(Sheet datatypeSheet) {
		Iterator<Row> iterator = datatypeSheet.iterator();
		LOGGER.info("Identificado " + datatypeSheet.getLastRowNum() + " registros de contas inseridos no arquivo...");
		LOGGER.info("Iniciando o processo de atualizacao para receita...");

		while (iterator.hasNext()) {
			Row currentRow = iterator.next();
			if (currentRow.getRowNum() == 0) {
				currentRow.createCell(4).setCellValue("retorno");
				continue;
			}
			sendUpdateToReceita(currentRow, datatypeSheet.getLastRowNum());
		}

		LOGGER.info("Todas as foram contas enviadas para atualizacao.");
	}

	public static void sendUpdateToReceita(Row currentRow, Integer rowsSize) {
		ContaDTO conta = parseCellsRow(currentRow);
		LOGGER.info("Enviando atualizacao da conta " + currentRow.getRowNum() + " de " + rowsSize);
		Boolean status = null;
		Integer retry = 1;

		while (status == null && retry <= 3) {
			try {
				status = receitaService.atualizarConta(conta.getAgencia().toString(), conta.getConta(), conta.getSaldo(), conta.getStatus());
			} catch (RuntimeException e) {
				LOGGER.info("Ocorreu uma falha na comunicacao. Tentativa " + retry + " de " + 3);
			} catch (InterruptedException e) {
				LOGGER.info("Ocorreu uma falha na comunicacao. Tentativa " + retry + " de " + 3);
			} finally {
				retry++;
			}
		}
		if (status == null) {
			LOGGER.info("Esgotadas todas as tentativas, tentando proximo registro.");
			status = false;
			currentRow.createCell(4).setCellValue(status.toString());
			return;
		}

		currentRow.createCell(4).setCellValue(status.toString());
		LOGGER.info("Conta enviada para atualizacao, retorno do servico: " + status);
	}

	public static void saveNewFile(Workbook workbook) throws IOException {
		LOGGER.info("Gravando novo documento com as informacoes de retorno...");
		File currDir = new File(".");
		String path = currDir.getAbsolutePath();
		String fileLocation = path.substring(0, path.length() - 1) + "contas-atualizadas.xlsx";
		FileOutputStream outputStream = new FileOutputStream(fileLocation);
		workbook.write(outputStream);
		workbook.close();
		LOGGER.info("Documento gravado com sucesso, diretorio do arquivo em: " + fileLocation);
	}

	public static ContaDTO parseCellsRow(Row row) {
		Double agencia = row.getCell(0).getNumericCellValue();
		String conta = row.getCell(1).getStringCellValue().replace("-", "");
		Double saldo = row.getCell(2).getNumericCellValue();
		String status = row.getCell(3).getStringCellValue();
		return new ContaDTO(agencia.intValue(), conta, saldo, status);
	}
}
