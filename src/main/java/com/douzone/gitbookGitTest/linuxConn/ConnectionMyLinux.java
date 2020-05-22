package com.douzone.gitbookGitTest.linuxConn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.saro.commons.ssh.SSHExecutor;

public class ConnectionMyLinux {
	private final static String host = "192.168.1.15";
	private final static int port = 22;
	private final static String user = "gitbook";
	private final static String password = "gitbook";
	private final static String charset = "utf-8";
	private final static String dir = "/var/www/git/";

	// 사용자 추가 bare 저장소 생성
	public static String userAdd(String userName) {
		try {
			SSHExecutor.just(host, port, user, password, charset, "sudo mkdir " + dir + userName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return userName;
	}

	// 유저의 repository 생성
	public static String addRepo(String userName, String repoName) {
		try {
			SSHExecutor.just(host, port, user, password, charset,
					"cd " + dir + userName + " && sudo git-create-repo " + userName + " " + repoName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "success";
	}

	public static String getResult(String command) {
		try {
			return SSHExecutor.just(host, port, user, password, charset, command);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	// 사용자 명의 폴더가 있는지 확인하기
	public static Boolean checkUser(String userName) {
		return ConnectionMyLinux.getResult("ls " + dir + userName).contains("ls: cannot access") == false;
	}

	/*
	 * 한 사용자의 레포지토리 목록 보여주기 [조건] RepositoryController에서 checkUser(userName) == true
	 * 임을 확인했다는 전제 하에 실행한다 !!
	 */
	public static List<String> getRepositoryList(String userName) {
		List<String> list = new ArrayList<>();
		list.addAll(Arrays.asList(ConnectionMyLinux.getResult("ls " + dir + userName).split("\\s+")));

		return list;
	}

	// 사용자 폴더 & 깃 레포지토리 폴더 여부 확인하기 (input 에서 레포지토리 명 뒤에 ".git" 붙이지 않는다)
	// ex) checkUserAndRepo("user03", "test09") <-- .../user03/test09.git/ 여부 확인
	public static Boolean checkUserAndRepo(String userName, String repoName) {
		return ConnectionMyLinux.getResult("ls " + dir + userName + "/" + repoName + ".git")
				.contains("ls: cannot access") == false;
	}
	
	public static String checkNewRepo(String userName, String repoName) {
		String result = null;
		try {
			result = SSHExecutor.just(host, port, user, password, charset,
					"cd " + dir + userName + "/" + repoName + ".git && git ls-tree --full-tree --name-status master");

		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/*
	 * 한 사용자/레포지토리의 파일/폴더 목록 제일 상단을 보여주기 [조건] RepositoryController에서
	 * checkUserAndRepo(userName, repoName) == true 임을 확인했다는 전제 하에 실행한다 !! dat {
	 * type: "folder", contents: { '0' : { path: commit: date: }, '1' : { path:
	 * commit: date: }, ... } }
	 */
	public static Map<String, Object> getFileListOnTop(String userName, String repoName) {
		Map<String, Object> result = new HashMap<>();
		result.put("type", "folder");

		Map<String, Object> contents = new HashMap<>();

		// contents 에다가 넣을 내용들을 가져온다.
		// [주의] path 끝에 "/" 을 넣는다 !!
		String[] fileList = ConnectionMyLinux.getResult(
				"cd " + dir + userName + "/" + repoName + ".git && git ls-tree --full-tree --name-status master")
				.split("\n");

		// 정석아 부탁한다 ㅜ
		for (int i = 0; i < fileList.length; i++) {
			// 임시 버퍼 Map을 정의
			Map<String, Object> buffer = new HashMap<>();

			// 1. 경로 path를 넣기
			buffer.put("path", fileList[i]);

			String[] dateAndCommit = ConnectionMyLinux.getResult("cd " + dir + userName + "/" + repoName
					+ ".git && git log -1 --pretty=format:\"%ar\t%s\" -- " + fileList[i]).split("\t");
			// 2. 경로 path에 해당되는 commit 메시지를 넣기
			// 해당 path의 commit 메시지 가져오기

			buffer.put("commit", dateAndCommit[1]);

			// 3. 경로 path의 일자 정보 넣기
			// 해당 path의 date 가져오기

			buffer.put("date", dateAndCommit[0]);

			// 4. 마지막으로 contents 에다가 {"i" : buffer 객체} 형태로 넣는다.
			contents.put(String.valueOf(i), buffer);
		}

		// contents 이름의 HashMap 객체를 넣기
		result.put("contents", contents);

		return result;
	}

	/*
	 * Path에 따라서 파일 내용이 or 폴더 내용을 보여주기 checkUserAndRepo(userName, repoName) == true
	 * 임을 확인했다는 전제 하에 실행한다 !!
	 * 
	 */
	public static Map<String, Object> getView(String userName, String repoName, String pathName) {
		// 1. path가 파일(blob) 인지 아니면 폴더(tree)인지 확인하기
		// [주의] path 끝에 "/" 뺀다 !!
		// infoLine[0] : 유형코드
		// infoLine[1] : 유형(blob/tree)
		// infoLine[2] : 해시코드
		// infoLine[3] : 파일 or 폴더 (경로 포함, 빈칸 있으면 잘릴 수도 있음...어차피 안쓰임)
		String infoLine = ConnectionMyLinux.getResult(
				"cd " + dir + userName + "/" + repoName + ".git && git ls-tree --full-tree master " + pathName);

		// 2. 조회되지 않은 경우(== 없다는 의미임) --> null을 보낸다
		if (infoLine == null || "".equals(infoLine)) {
			return null;
		}

		Map<String, Object> result = new HashMap<>();
		String[] info = infoLine.split("\\s+");

		// 3-1. 파일(blob)인 경우
		if ("blob".equals(info[1])) {
			result.put("type", "file");
			result.put("contents", ConnectionMyLinux
					.getResult("cd " + dir + userName + "/" + repoName + ".git && git cat-file -p " + info[2]));
		}
		// 3-2. 폴더(tree)인 경우
		else if ("tree".equals(info[1])) {
			result.put("type", "folder");
			Map<String, Object> contents = new HashMap<>();

			// contents 에다가 넣을 내용들을 가져온다.
			// [주의] path 끝에 "/" 을 넣는다 !!
			String[] fileList = ConnectionMyLinux.getResult("cd " + dir + userName + "/" + repoName
					+ ".git && git ls-tree --full-tree --name-status master " + pathName + "/").split("\n");

			// 정석아 부탁한다 ㅜ
			for (int i = 0; i < fileList.length; i++) {
				// 임시 버퍼 Map을 정의
				Map<String, Object> buffer = new HashMap<>();

				// 1. 경로 path를 넣기
				buffer.put("path", fileList[i]);

				String[] dateAndCommit = ConnectionMyLinux.getResult("cd " + dir + userName + "/" + repoName
						+ ".git && git log -1 --pretty=format:\"%ar\t%s\" -- " + fileList[i]).split("\t");
				// 2. 경로 path에 해당되는 commit 메시지를 넣기
				// 해당 path의 commit 메시지 가져오기

				buffer.put("commit", dateAndCommit[1]);

				// 3. 경로 path의 일자 정보 넣기
				// 해당 path의 date 가져오기

				buffer.put("date", dateAndCommit[0]);

				// 4. 마지막으로 contents 에다가 {"i" : buffer 객체} 형태로 넣는다.
				contents.put(String.valueOf(i), buffer);
			}

			// contents 이름의 HashMap 객체를 넣기
			result.put("contents", contents);
		}
		return result;
	}

}
