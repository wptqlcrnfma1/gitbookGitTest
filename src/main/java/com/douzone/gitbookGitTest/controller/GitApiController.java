package com.douzone.gitbookGitTest.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import com.douzone.gitbookGitTest.dto.JsonResult;
import com.douzone.gitbookGitTest.linuxConn.ConnectionMyLinux;

@RestController("gitApiController")
@RequestMapping("/mytimeline")
public class GitApiController {

	@ResponseBody
	@GetMapping("/회원가입/{userName}")
	public JsonResult addUser(@PathVariable("userName") String userName) {
		ConnectionMyLinux.userAdd(userName);
		return JsonResult.success("success");
	}

	@ResponseBody
	@GetMapping("/add/{userName}/{repoName}")
	public JsonResult addRepo(@PathVariable("userName") String userName, @PathVariable("repoName") String repoName) {
		String result = ConnectionMyLinux.addRepo(userName, repoName);
		return JsonResult.success(result);
	}

	@ResponseBody
	@GetMapping("/view/{userName}")
	public JsonResult showRepoListOfUser(@PathVariable("userName") String userName) {
		if (ConnectionMyLinux.checkUser(userName) == false) {
			return JsonResult.fail("user not found");
		}
		return JsonResult.success(ConnectionMyLinux.getRepositoryList(userName));
	}

	// 커밋필요!
	@ResponseBody
	@GetMapping("/view/{userName}/{repoName}")
	public JsonResult showRootOnRepo(@PathVariable("userName") String userName,
			@PathVariable("repoName") String repoName) {

		// 잘못된 URL 입력
		if (ConnectionMyLinux.checkUserAndRepo(userName, repoName) == false) {
			return JsonResult.fail("repo not found");
		}

		// 최초 만든 repo일 경우
		if (ConnectionMyLinux.checkNewRepo(userName, repoName).contains("fatal: Not a valid object name master")) {
			return JsonResult.fail("newRepo");
		}

		return JsonResult.success(ConnectionMyLinux.getFileListOnTop(userName, repoName));
	}

	@ResponseBody
	@GetMapping("/view/{userName}/{repoName}/**")
	public JsonResult showInternalOnRepo(@PathVariable("userName") String userName,
			@PathVariable("repoName") String repoName, HttpServletRequest request) {
		String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

		String pathName = fullPath.substring(fullPath.indexOf(repoName) + repoName.length() + 1);

		if ("/".equals(pathName.substring(pathName.length() - 1, pathName.length()))) {
			pathName = pathName.substring(0, pathName.length() - 1);
		}

		Map<String, Object> data = ConnectionMyLinux.getView(userName, repoName, pathName);
		if (data == null) {
			return JsonResult.fail("cannot retrieve internal contents");
		}
		return JsonResult.success(data);
	}

}
